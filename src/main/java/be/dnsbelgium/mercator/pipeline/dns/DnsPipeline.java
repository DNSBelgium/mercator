package be.dnsbelgium.mercator.pipeline.dns;

import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.VisitRequestModule;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.common.VisitRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** DNS crawling module — mirrors {@code WebPipeline} to prove the abstraction. */
@Component
public class DnsPipeline extends VisitRequestModule<DnsCrawlResult> {

    private final DnsCrawlService dnsCrawlService;

    public DnsPipeline(JdbcClient jdbcClient,
                       @Qualifier(PipelineJacksonConfig.PIPELINE_OBJECT_MAPPER) ObjectMapper objectMapper,
                       PipelineExecutors executors,
                       PipelineProperties properties,
                       MeterRegistry meterRegistry,
                       DnsCrawlService dnsCrawlService,
                       ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, itemSourceFactory);
        this.dnsCrawlService = dnsCrawlService;
    }

    @Override
    public String name() {
        return "dns";
    }

    @Override
    protected ItemProcessor<VisitRequest, DnsCrawlResult> processor() {
        return dnsCrawlService;
    }

    @Override
    protected Class<DnsCrawlResult> outputType() {
        return DnsCrawlResult.class;
    }
}
