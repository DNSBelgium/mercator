package be.dnsbelgium.mercator.pipeline.module;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemWriter;
import be.dnsbelgium.mercator.pipeline.service.PipelineService;
import be.dnsbelgium.mercator.pipeline.simulation.FileWriterService;
import be.dnsbelgium.mercator.pipeline.simulation.GeneratingItemSource;
import be.dnsbelgium.mercator.pipeline.simulation.SimulatedProcessor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Default demo module: exercises the generic engine with synthetic {@code String} work and
 * writes plain-text batch files. Useful for benchmarking and as a smoke test when no real
 * data source is configured.
 */
@Component
public class SimulatedPipeline implements PipelineModule {

    private final PipelineExecutors executors;
    private final PipelineProperties properties;
    private final MeterRegistry meterRegistry;

    public SimulatedPipeline(PipelineExecutors executors,
                             PipelineProperties properties,
                             MeterRegistry meterRegistry) {
        this.executors = executors;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public long run() {
        ItemSource<String> source = new GeneratingItemSource(0, properties.getSimulatedItemCount());
        ItemProcessor<String, String> processor = new SimulatedProcessor();
        ItemWriter<String> writer =
                new FileWriterService(Path.of(properties.getOutputDirectory(), name()), properties.getBatchSize());

        PipelineService<String, String> pipeline =
                new PipelineService<>(name(), source, processor, writer, executors, properties, meterRegistry);
        return pipeline.runPipeline(properties.getNumConsumers());
    }
}

