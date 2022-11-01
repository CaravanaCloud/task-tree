package cj;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;

public class CloudJanitor {
    private static final Logger log = LoggerFactory.getLogger(CloudJanitor.class);

    @Inject
    Tasks tasks;

    @Inject
    LaunchMode launchMode;

    public int run(String task, List<String> inputs){
        log.trace("CloudJanitor.run()");
        try {
            tasks.run(task, inputs);
        } catch (Exception e) {
            log.error("CloudJanitor.run() failed", e);
            return -1;
        }
        return 0;
    }

    void onStart(@Observes StartupEvent ev) {
        log.info("Thank you for running cloud-janitor.");
        log.debug("Quarkus launch mode: {}", launchMode);
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.debug("Cloud Janitor stopped.");
    }
}