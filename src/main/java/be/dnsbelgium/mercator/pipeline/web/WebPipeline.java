package be.dnsbelgium.mercator.pipeline.web;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineJacksonConfig;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.VisitRequestModule;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.web.WebProcessor;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Web crawling module: reads {@link VisitRequest}s from CSV (or the Postgres work queue
 * under the {@code postgres-queue} profile), crawls each with {@link WebProcessor}, and
 * writes {@link WebCrawlResult}s as JSON-then-Parquet.
 */
@Component
public class WebPipeline extends VisitRequestModule<WebCrawlResult> {

    private final WebProcessor webProcessor;

    public WebPipeline(JdbcClient jdbcClient,
                       @Qualifier(PipelineJacksonConfig.PIPELINE_OBJECT_MAPPER) ObjectMapper objectMapper,
                       PipelineExecutors executors,
                       PipelineProperties properties,
                       MeterRegistry meterRegistry,
                       WebProcessor webProcessor,
                       WebRepository repository,
                       ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, repository, itemSourceFactory);
        this.webProcessor = webProcessor;
    }


    @Override
    public String name() {
        return "web";
    }

    @Override
    protected ItemProcessor<VisitRequest, be.dnsbelgium.mercator.web.domain.WebCrawlResult> processor() {
        return webProcessor;
    }

    @Override
    protected Class<be.dnsbelgium.mercator.web.domain.WebCrawlResult> outputType() {
        return be.dnsbelgium.mercator.web.domain.WebCrawlResult.class;
    }


}
