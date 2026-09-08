package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.common.VisitRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
@Slf4j
public class CsvItemSource implements ItemSource<VisitRequest> {

    private final JdbcClient jdbcClient;
    private final String filePath;
    private boolean done = false;

    public CsvItemSource(JdbcClient jdbcClient, String filePath) {
        this.filePath = filePath;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<VisitRequest> getItems() {
        log.info("reading items from CSV file: {}", filePath);
        List<VisitRequest> visits = jdbcClient
                .sql("select domain_name, visit_id from read_csv_auto(?)")
                .param(filePath)
                .query((row, _) -> {
                    VisitRequest visitRequest = new VisitRequest();
                    visitRequest.setDomainName(row.getString("domain_name"));
                    visitRequest.setVisitId(row.getString("visit_id"));
                    return visitRequest;
                })
                .list();
        log.info("We have read {} items from CSV file: {}", visits.size(), filePath);
        this.done = true;
        return visits;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    /** One-shot source: it is {@link #isDone() done} after the first poll, so it never sleeps. */
    @Override
    public boolean sleepBetweenPolls() {
        return false;
    }
}
