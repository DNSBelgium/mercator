package be.dnsbelgium.mercator.pipeline;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.PipelineModule;
import be.dnsbelgium.mercator.pipeline.queue.QueueModuleRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

//@SpringBootApplication
@Slf4j
@Component
public class PipelineApplication {

    private final Map<String, PipelineModule> modulesByName;
    private final PipelineProperties properties;

    /** Present only under the {@code postgres-queue} profile → run modules continuously. */
    private final ObjectProvider<QueueModuleRunner> queueModuleRunner;

    public PipelineApplication(List<PipelineModule> modules,
                               PipelineProperties properties,
                               ObjectProvider<QueueModuleRunner> queueModuleRunner) {
        this.properties = properties;
        this.queueModuleRunner = queueModuleRunner;
        this.modulesByName = new LinkedHashMap<>();
        for (PipelineModule module : modules) {
            this.modulesByName.put(module.name().toLowerCase(Locale.ROOT), module);
        }
    }

    static void main(String[] args) {
        //SpringApplication.run(PipelineApplication.class, args);
    }

    //@EventListener(ApplicationReadyEvent.class)
    public void run() {
        List<String> requested = properties.getModules();
        log.info("Requested modules: {} (available: {})", requested, modulesByName.keySet());

        List<PipelineModule> resolved = resolveModules(requested);

        QueueModuleRunner runner = queueModuleRunner.getIfAvailable();
        if (runner != null) {
            // Stateful (postgres-queue) mode: run bounded passes round-robin, forever.
            log.info("postgres-queue profile active: launching continuous module runner");
            runner.start(resolved);
            return;
        }

        // Stateless mode: run each requested module once, to completion.
        for (PipelineModule module : resolved) {
            long start = System.currentTimeMillis();
            long produced = module.run();
            log.info("Module '{}' completed in {} ms (produced {} items)",
                    module.name(), System.currentTimeMillis() - start, produced);
        }
    }

    /** Resolves the requested module names to beans in order, logging and skipping unknowns. */
    private List<PipelineModule> resolveModules(List<String> requested) {
        List<PipelineModule> resolved = new ArrayList<>();
        for (String moduleName : requested) {
            PipelineModule module = modulesByName.get(moduleName.toLowerCase(Locale.ROOT));
            if (module == null) {
                log.error("Unknown module '{}'. Available modules: {}", moduleName, modulesByName.keySet());
                continue;
            }
            resolved.add(module);
        }
        return resolved;
    }

}
