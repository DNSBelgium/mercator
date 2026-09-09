package be.dnsbelgium.mercator.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the new (non–Spring Batch) pipeline implementation once the application is ready,
 * but only when the legacy Spring Batch engine is <em>not</em> selected.
 *
 * <p>The implementation is chosen with the {@code mercator.batch.enabled} boolean:
 * <ul>
 *   <li>{@code false} (default) &rarr; this runner starts {@link PipelineApplication#run()};</li>
 *   <li>{@code true} &rarr; the legacy {@link be.dnsbelgium.mercator.JobRunner} runs instead
 *       (see {@code application-batch.properties}, which sets the flag to {@code true}).</li>
 * </ul>
 *
 * <p>Guarded with {@code @Profile("!test")} so that {@code @SpringBootTest} contexts never
 * kick off a real crawl on startup.
 */
@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(name = "mercator.batch.enabled", havingValue = "false", matchIfMissing = true)
public class PipelineRunner {

    private final PipelineApplication pipelineApplication;

    public PipelineRunner(PipelineApplication pipelineApplication) {
        this.pipelineApplication = pipelineApplication;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("mercator.batch.enabled=false => starting the new pipeline implementation");
        pipelineApplication.run();
    }
}

