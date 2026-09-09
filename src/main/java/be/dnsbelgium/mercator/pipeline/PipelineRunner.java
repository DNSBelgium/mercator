package be.dnsbelgium.mercator.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the pipeline implementation once the application is ready.
 *
 * <p>Since the legacy Spring Batch engine has been removed, the pipeline is the only crawl engine.
 * Guarded with {@code @Profile("!test")} so that {@code @SpringBootTest} contexts never kick off a
 * real crawl on startup.
 */
@Slf4j
@Component
@Profile("!test")
public class PipelineRunner {

    private final PipelineApplication pipelineApplication;

    public PipelineRunner(PipelineApplication pipelineApplication) {
        this.pipelineApplication = pipelineApplication;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready => starting the pipeline implementation");
        pipelineApplication.run();
    }
}

