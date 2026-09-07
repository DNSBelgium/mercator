package be.dnsbelgium.mercator.pipeline.dns;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.VisitRequestModule;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.pipeline.service.VisitRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** DNS crawling module — mirrors {@code WebPipeline} to prove the abstraction. */
@Component
public class DnsPipeline extends VisitRequestModule<DnsResult> {

    private final DnsProcessor dnsProcessor;

    public DnsPipeline(JdbcClient jdbcClient,
                       ObjectMapper objectMapper,
                       PipelineExecutors executors,
                       PipelineProperties properties,
                       MeterRegistry meterRegistry,
                       DnsProcessor dnsProcessor,
                       ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, itemSourceFactory);
        this.dnsProcessor = dnsProcessor;
    }

    @Override
    public String name() {
        return "dns";
    }

    @Override
    protected ItemProcessor<VisitRequest, DnsResult> processor() {
        return dnsProcessor;
    }

    @Override
    protected Class<DnsResult> outputType() {
        return DnsResult.class;
    }
}
