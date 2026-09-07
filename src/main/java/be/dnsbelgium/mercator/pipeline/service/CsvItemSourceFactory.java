package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.queue.DatabaseItemSourceFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Default {@link ItemSourceFactory} that produces one-shot {@link CsvItemSource}s reading
 * from {@code pipeline.input-csv}. It is always registered as a bean, and it is the source
 * every {@code VisitRequestModule} uses unless the {@code postgres-queue} profile activates
 * {@link DatabaseItemSourceFactory}, which is {@code @Primary}
 * and therefore wins autowiring when present.
 */
@Component
public class CsvItemSourceFactory implements ItemSourceFactory<ItemSource<VisitRequest>> {

    private final JdbcClient jdbcClient;
    private final PipelineProperties properties;

    public CsvItemSourceFactory(JdbcClient jdbcClient, PipelineProperties properties) {
        this.jdbcClient = jdbcClient;
        this.properties = properties;
    }

    @Override
    public ItemSource<VisitRequest> create(String moduleName) {
        return new CsvItemSource(jdbcClient, properties.getInputCsv());
    }
}
