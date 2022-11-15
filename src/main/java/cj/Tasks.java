package cj;

import cj.ocp.CapabilityNotFoundException;
import cj.reporting.Reporting;
import cj.spi.Task;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static cj.Errors.Type.Message;

@ApplicationScoped
public class Tasks {
    static final LocalDateTime startTime = LocalDateTime.now();

    @Inject
    Logger log;

    @Inject
    BeanManager bm;

    @Inject
    Configuration config;

    @Inject
    RateLimiter rateLimiter;

    @Inject
    Reporting reporting;

    Set<Capabilities> capabilities = new HashSet<>();

    Map<String, String> cliInputs = new HashMap<>();

    List<Task> history = new ArrayList<>();

    String task;

    public void run() {
        log.trace("Tasks.run()");
        log.debug("Capabilities: {}", getCapabilities());
        //parseInputs(inputs);
        var tasks = loadTasks();
        var taskNames = String.join(",", tasks.stream().map(TaskConfiguration::name).toList());
        //var inputsSize = inputs != null ? inputs.size() : 0;
        // log.info("Starting {} tasks with {} inputs and dry run {}.", tasks.size(), inputsSize, dryRun);
        //if (inputsSize > 0)
        //    log.debug("Inputs: {}", inputs);
        for (var task : tasks) {
            run(task);
        }
        report();
    }

    private List<TaskConfiguration> loadTasks() {
        var tasks = new ArrayList<>(config.tasks());
        config.task().ifPresent(t -> addTaskByName(tasks, t));
        if (task != null) {
            addTaskByName(tasks, task);
        }
        return tasks;
    }

    private void addTaskByName(List<TaskConfiguration> tasks, String taskName) {
        if (taskName != null && !taskName.isEmpty()) {
            tasks.add(new SimpleTaskConfiguration(taskName));
        }
    }

    private void parseInputs(List<String> inputs) {
        if (inputs != null) for (var input : inputs) {
            var parts = input.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid input: " + input);
            }
            var key = parts[0];
            var value = parts[1];
            cliInputs.put(key, value);
        }
    }

    private void run(TaskConfiguration task) {
        log.info("Running task: {}", task.name());
        var matches = lookupTasks(task.name());
        runAll(matches);
    }

    private void report() {
        if (config.report().enabled())
            try {
                reporting.report(this);
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Reporting failed", ex);
            }
    }

    private void runAll(List<Task> matches) {
        matches.forEach(this::submit);
    }

    private List<Task> lookupTasks(String taskName) {
        var tasks = bm.getBeans(taskName)
                .stream()
                .map(this::fromBean)
                .toList();
        log.debug("Loaded {} tasks with name {} ", tasks.size(), taskName);
        return tasks;
    }

    //TODO: Consider async execution
    public Task submit(Task task) {
        task.init();
        var thisInputs = task.getInputs();
        var dependencies = task.getDependencies();
        dependencies.forEach(d -> {
            //TODO: Consider if dependencies should inherit inputs
            d.getInputs().putAll(thisInputs);
            submit(d);
        });
        runSingle(task);
        return task;
    }


    private Task fromBean(Bean<?> bean) {
        var ctx = bm.createCreationalContext(bean);
        var ref = bm.getReference(bean, bean.getBeanClass(), ctx);
        if (ref instanceof Task task) {
            return task;
        } else {
            log.error("Bean {} is not a Task", bean);
            throw new IllegalArgumentException("Bean is not a task");
        }
    }

    //TODO: Consider retries
    //TODO: Consider thread synchronization
    public void runSingle(Task task) {
        history.add(task);
        try {
            task.setStartTime(LocalDateTime.now());
            task.apply();
            log.trace("Executed {} ({})",
                    task,
                    task.isWrite() ? "W" : "R");
            //TODO: General waiter
            if (task.isWrite()) {
                rateLimiter.waitAfterTask(task);
            }
        }catch (CapabilityNotFoundException e){
            var c = e.getCapability();
            log.warn("Capability not found: {}, try with -c '{}' or equivalent", c, c);
        }catch (ConfigurationNotFoundException e){
            var varName = e.getVarName();
            log.warn("Configuration not found: {}.", varName);
        } catch (Exception e) {
            task.getErrors().put(Message, e.getMessage());
            log.error("Error executing {}: {}", task, e.getMessage());
            // e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            task.setEndTime(LocalDateTime.now());
        }
    }

    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }

    public String getStartTimeFmt() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(startTime);
    }

    public Configuration getConfig() {
        return config;
    }


    private String executionId;

    public synchronized String getExecutionId() {
        if (executionId == null) {
            var sdf = new SimpleDateFormat("yyMMddHHmmss");
            executionId = "cj" + sdf.format(new Date());
        }
        return executionId;
    }


    public String getCLIInput(String key) {
        return cliInputs.get(key);
    }

    public void setTask(String taskName) {
        this.task = taskName;
    }

    public void addInput(String entry) {
        var parts = entry.split("=");
        var key = parts[0];
        var hasValue = parts.length > 1;
        var value = hasValue ? parts[1] : "";
        cliInputs.put(key, value);
    }

    public void addCapability(String capability) {
        if ("all".equalsIgnoreCase(capability)) {
            var caps = List.of(Capabilities.values());
            capabilities.addAll(caps);
        } else if ("none".equalsIgnoreCase(capability)) {
            capabilities.clear();
        } else {
            try{
                var cap = Capabilities.valueOf(capability);
                capabilities.add(cap);
            } catch (IllegalArgumentException ex) {
                log.error("Invalid capability: {}", capability);
            }
        }
    }

    public Set<Capabilities> getCapabilities() {
        return capabilities;
    }

    public boolean hasCapabilities(Capabilities[] cs) {
        for (var c : cs) {
            if (!capabilities.contains(c)) {
                return false;
            }
        }
        return true;
    }

    public void addAll(List<Capabilities> capabilities) {
        this.capabilities.addAll(capabilities);
    }
}
