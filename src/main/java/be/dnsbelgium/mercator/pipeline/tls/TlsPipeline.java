package be.dnsbelgium.mercator.pipeline.tls;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.VisitRequestModule;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TlsPipeline extends VisitRequestModule<TlsCrawlResult> {

    private final TlsCrawler tlsCrawler;

    public TlsPipeline(JdbcClient jdbcClient,
                       ObjectMapper objectMapper,
                       PipelineExecutors executors,
                       PipelineProperties properties,
                       MeterRegistry meterRegistry,
                       TlsCrawler tlsCrawler,
                       ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, itemSourceFactory);
        this.tlsCrawler = tlsCrawler;
    }


    @SneakyThrows
    @Override
    protected ItemProcessor<VisitRequest, TlsCrawlResult> processor() {
        return tlsCrawler;
    }

    @Override
    protected Class<TlsCrawlResult> outputType() {
        return TlsCrawlResult.class;
    }

    @Override
    public String name() {
        return "tls";
    }
}
