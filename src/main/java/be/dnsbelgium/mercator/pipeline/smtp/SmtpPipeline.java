package be.dnsbelgium.mercator.pipeline.smtp;

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

/** SMTP crawling module — mirrors {@code WebPipeline} to prove the abstraction. */
@Component
public class SmtpPipeline extends VisitRequestModule<SmtpResult> {

    private final SmtpProcessor smtpProcessor;

    public SmtpPipeline(JdbcClient jdbcClient,
                        ObjectMapper objectMapper,
                        PipelineExecutors executors,
                        PipelineProperties properties,
                        MeterRegistry meterRegistry,
                        SmtpProcessor smtpProcessor,
                        ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, itemSourceFactory);
        this.smtpProcessor = smtpProcessor;
    }

    @Override
    public String name() {
        return "smtp";
    }

    @Override
    protected ItemProcessor<VisitRequest, SmtpResult> processor() {
        return smtpProcessor;
    }

    @Override
    protected Class<SmtpResult> outputType() {
        return SmtpResult.class;
    }
}
