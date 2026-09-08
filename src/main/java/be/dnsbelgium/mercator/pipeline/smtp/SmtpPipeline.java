package be.dnsbelgium.mercator.pipeline.smtp;

import be.dnsbelgium.mercator.pipeline.config.PipelineExecutors;
import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.VisitRequestModule;
import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.smtp.SmtpCrawler;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** SMTP crawling module — mirrors {@code WebPipeline} to prove the abstraction. */
@Component
public class SmtpPipeline extends VisitRequestModule<SmtpVisit> {

    private final SmtpCrawler smtpCrawler;

    public SmtpPipeline(JdbcClient jdbcClient,
                        ObjectMapper objectMapper,
                        PipelineExecutors executors,
                        PipelineProperties properties,
                        MeterRegistry meterRegistry,
                        SmtpCrawler smtpCrawler,
                        ItemSourceFactory<ItemSource<VisitRequest>> itemSourceFactory) {
        super(jdbcClient, objectMapper, executors, properties, meterRegistry, itemSourceFactory);
        this.smtpCrawler = smtpCrawler;
    }

    @Override
    public String name() {
        return "smtp";
    }

    @Override
    protected ItemProcessor<VisitRequest, SmtpVisit> processor() {
        return smtpCrawler::process;
    }

    @Override
    protected Class<SmtpVisit> outputType() {
        return SmtpVisit.class;
    }
}
