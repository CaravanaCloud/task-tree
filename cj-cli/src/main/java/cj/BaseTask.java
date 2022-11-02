package cj;

import cj.spi.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static cj.Errors.Type;
import static cj.Errors.Type.Message;
@Dependent
public class BaseTask implements Task {
    @Inject
    transient Tasks tasks;

    @Inject
    Configuration config;

    //TODO: Use null instead of Optional in fields
    Optional<LocalDateTime> startTime = Optional.empty();
    Optional<LocalDateTime> endTime = Optional.empty();

    Map<Input, Object> inputs = new HashMap<>();
    Map<Output, Object> outputs = new HashMap<>();
    Map<Errors, Object> errors = new HashMap<>();

    /* Submits a delegate task for execution */
    public Task submit(Task delegate){
        delegate.getInputs().putAll(getInputs());
        return tasks.submit(delegate);
    }

    public Task submit(Task delegate, Input input, Object value){
        return tasks.submit(delegate.withInput(input, value));
    }


    /* Task Interface Methods */
    @Override
    public Optional<LocalDateTime> getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(LocalDateTime localDateTime) {
        startTime = Optional.ofNullable(localDateTime);
    }

    @Override
    public Optional<LocalDateTime> getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(LocalDateTime localDateTime) {
        this.endTime = Optional.ofNullable(localDateTime);
    }

    /* Input, Output and Errors */

    public Optional<Object> output(Output key) {
        var result = Optional.ofNullable(getOutputs().get(key));
        if (result.isEmpty()){
            for (Task dep: getDependencies()){
                result = dep.output(key);
                if (result.isPresent()){
                    return result;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> outputList(Output key, Class<T> valueClass) {
        return output(key)
                .map(o -> (List<T>) o)
                .orElse(List.of());
    }
    @SuppressWarnings("unchecked")
    public <T> List<T> inputList(Input key, Class<T> valueClass) {
        return input(key)
                .map(o -> (List<T>) o)
                .orElse(List.of());
    }


    @Override
    public Optional<String> outputString(Output key) {
        return output(key)
                .map(o -> o.toString());
    }

    @Override
    public Map<Output, Object> getOutputs() {
        return outputs;
    }

    @Override
    public Map<Errors, Object> getErrors() {
        return errors;
    }

    protected void success(Output key, Object value){
        output(key, value);
    }

    protected void success(){
        trace("Task success(): {}", this);
    }

    /* Logging Shortcuts */
    protected void info(String message, Object... args){
        logger().info(fmt(message), args);
    }
    protected void trace(String message, Object... args){
        logger().trace(fmt(message), args);
    }
    protected void debug(String message, Object... args){
        logger().debug(fmt(message), args);
    }

    protected void error(String message, Object... args){
        logger().error(fmt(message), args);
    }

    protected RuntimeException fail(String message) {
        error(message);
        getErrors().put(Message ,message);
        return new RuntimeException(message);
    }

    private String fmt(String message) {
        return getContextString() + getContextSeparator() + message;
    }

    protected String getContextString() {
        return "";
    }

    private String getContextSeparator() {
        return " || ";
    }

    protected void fail(Exception ex) {
        logger().error(ex.getMessage(), ex);
        if( Configuration.PRINT_STACK_TRACE){
            ex.printStackTrace();
        }
        getErrors().put(Type.Exception , ex);
        throw new RuntimeException(ex);
    }

    protected void warn(String message, Object... args) {
        logger().warn(fmt(message), args);
    }
    protected void warn(Exception ex, String message, Object... args) {
        warn(ex.getMessage());
        warn(fmt(message), args);
    }


    protected Logger logger() {
        return LoggerFactory.getLogger(getLoggerName());
    }

    @Override
    public String toString() {
        return  "%s ".formatted(
                getSimpleName());

    }

    public Configuration getConfig() {
        return config;
    }


    @Override
    public Map<Input, Object> getInputs(){
        return inputs;
    }

    public String expectInputString(Input key){
        return inputString(key).orElseThrow();
    }

    public String getInputString(Input key){
        return getInputString(key, null);
    }

    public String getInputString(Input key, String defaultValue){
        return inputAs(key, String.class).orElse(defaultValue);
    }

    public Task withInput(Input key, Object value) {
        inputs.put(key, value);
        return this;
    }

    public Task withInputs(Map<Input, Object> inputs) {
        this.inputs = inputs;
        return this;
    }

    public Object withInput(Input key) {
        return inputs.get(key);
    }

    public Optional<Object> input(Input key){
        if (key == null) return Optional.empty();
        var value = inputs.get(key);
        if (value == null) {
            var configInputs = getConfig().inputs();
            var keyName = key.toString();
            value = configInputs.get(keyName);
        }
        if (value == null) {
            value = tasks.getCLIInput(key.toString());
        }
        return Optional.ofNullable(value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> inputAs(Input key, Class<T> clazz){
        return (Optional<T>) input(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> outputAs(Output key, Class<T> clazz){
        return (Optional<T>) Optional.ofNullable(outputs.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getInput(Input key, Class<T> inputClass){
        return (T) input(key).get();
    }

    public String matchMark(boolean match){
        return match ? "X" : "O";
    }

    public List<Task> delegateAll(Task... tasks) {
        return Stream.of(tasks)
                .map(t -> t.withInputs(getInputs()))
                .toList();
    }

    public Task delegate(Task task){
        return task.withInputs(getInputs());
    }

    public void submitAll(Task... delegates){
        Stream.of(delegates).forEach(this::submit);
    }
    public void submitAll(List<Task> delegates){
        delegates.stream().forEach(this::submit);
    }

    public Optional<String> inputString(Input key){
        return input(key).map(o -> o.toString());
    }

    public Object output(Output key, Object value){
        if (value instanceof Optional<?> opt){
            if(opt.isPresent()){
                value = opt.get();
            }else{
                value = null;
            }
        }
        if (value != null) {
            trace("{} / {} := {}", toString(), key.toString(), value.toString());
            return outputs.put(key, value);
        } else return null;
    }

    public String getOutputString(Output key){
        return output(key).map(o -> o.toString()).get();
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    protected String getExecutionId(){
        return tasks.getExecutionId();
    }
}
