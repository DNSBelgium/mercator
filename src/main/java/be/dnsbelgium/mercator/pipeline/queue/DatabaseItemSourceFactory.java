package be.dnsbelgium.mercator.pipeline.queue;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemSourceFactory;
import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Creates module-scoped {@link DatabaseItemSource}s bound to the dedicated Postgres
 * {@code queueJdbcClient}. A single factory bean (present only under the
 * {@code postgres-queue} profile) lets each {@code VisitRequestModule} obtain its own
 * source via {@link #create(String)} with its module name, so the sources stay per-module
 * even though the queue client and config are shared singletons.
 */
@Slf4j
@Component
@Primary
@Profile("postgres-queue")
public class DatabaseItemSourceFactory implements ItemSourceFactory<ItemSource<VisitRequest>> {

    private final JdbcClient queueJdbcClient;
    private final PipelineProperties properties;

    public DatabaseItemSourceFactory(@Qualifier("queueJdbcClient") JdbcClient queueJdbcClient,
                                     PipelineProperties properties) {
        this.queueJdbcClient = queueJdbcClient;
        this.properties = properties;
    }

    /** Builds a stateful source that leases only {@code crawler_module = crawlerModule} rows.
     *  If it turns out to be needed, we could create separate PipelineProperties for each crawlerModule. */
    @Override
    public ItemSource<VisitRequest> create(String crawlerModule) {
        log.info("Creating DatabaseItemSource for module '{}'", crawlerModule);
        return new DatabaseItemSource(crawlerModule, queueJdbcClient, properties.getQueue());
    }
}
