package eu.bosteels.mercator.mono.scheduling;

import be.dnsbelgium.mercator.DuckDataSource;
import be.dnsbelgium.mercator.common.VisitRequest;
import eu.bosteels.mercator.mono.persistence.Repository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@SuppressWarnings("SqlDialectInspection")
@Component
@EnableScheduling
public class Scheduler {

    private final JdbcTemplate jdbcTemplate;
    private final WorkQueue workQueue;
    private final Repository repository;
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final AtomicBoolean ingestionStopped = new AtomicBoolean(false);

    @Value("${visits.ingested.directory}")
    private File ingestedDirectory;

    @Value("${visits.work.directory}")
    private File workDirectory;

    // TODO: disable this class when SQS is used

    @Autowired
    public Scheduler(DuckDataSource dataSource, WorkQueue workQueue, Repository repository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.workQueue = workQueue;
        this.repository = repository;
        logger.info("Scheduler created: {}", this);
    }

    @PostConstruct
    public void init() {
        logger.info("Scheduler ingestedDirectory: {}", ingestedDirectory);
        logger.info("Scheduler workDirectory: {}", ingestedDirectory);
        makeDirectories();
        // create empty parquet file in workDirectory
        String create_file = """
                copy
                    (select 'abc.be' as domain_name, 'visit_id' as visit_id where false)
                to '%s/.empty_file_do_not_remove.parquet'
                """.formatted(workDirectory.getAbsolutePath());
        logger.info("create_file: {}", create_file);
        jdbcTemplate.execute(create_file);
    }

    private void makeDirectories() {
        if (ingestedDirectory == null) {
            logger.warn("ingestedDirectory == null => not initializing it");
        } else {
            mkDir(ingestedDirectory);
        }
        if (workDirectory == null) {
            logger.warn("workDirectory == null => not initializing it");
        } else {
            mkDir(workDirectory);
        }
    }

    private void mkDir(File dir) {
        boolean dirCreated = dir.mkdirs();
        logger.debug("mkdirs {} created => {}", dir, dirCreated);
    }


    /*
    findFilesToIngest
    * reads parquet files and copies content to ingested table
    * copies (visit_id, domain_name) from ingested table to work table
    * moves ingested files to other folder

    queueWork
    * reads (visit_id, domain_name) rows from work table
    * sends them to the visit_requests queue (=> JMS message will be processed by Worker)

    Worker.receiveMessage() should remove row from work and add it to table done

     */

    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void findFilesToIngest() {
        if (ingestionStopped.get()) {
            logger.warn("Ingestion is stopped, check the logs for errors");
            return;
        }
        // TODO: move this to Repository class
        var ingestion_id = repository.nextid();
        var insert = """
            insert into ingested(ingestion_id, visit_id, domain_name, filename, ingested_at)
            select ?, visit_id, domain_name, filename, current_timestamp
            from read_parquet('%s/*.parquet', filename=True, union_by_name=True)
            where visit_id not in (select visit_id from done)
        """.formatted(workDirectory.getAbsolutePath());
        logger.debug("insert: {}", insert);
        int rowsInserted = jdbcTemplate.update(insert, ingestion_id);

        if (rowsInserted == 0) {
            logger.debug("No files to ingest found");
            return;
        }

        logger.info("rows inserted in ingestion: {}", rowsInserted);
        int workAdded = jdbcTemplate.update("""
            insert into work(visit_id, domain_name)
            select distinct visit_id, domain_name
            from ingested
            where ingestion_id = ?
            and visit_id not in (select visit_id from done)
            and visit_id not in (select visit_id from work)
        """, ingestion_id);
        logger.info("Added {} rows to work table", workAdded);
        List<String> fileNames = jdbcTemplate.queryForList("""
            select distinct filename
            from ingested
            where ingestion_id = ?
         """, String.class, ingestion_id);
        logger.info("Now moving {} ingested files", fileNames.size());
        for (String fileName : fileNames) {
            moveFile(fileName);
        }
        queueWork();
    }


    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void queueWork() {
        // adapt limit to number of messages on the queue
        int limit = 1000 - workQueue.getApproximateQueueSize();
        // TODO move code to Repository
        var list = jdbcTemplate.queryForList(
                "select visit_id, domain_name from work limit " + limit);
        for (Map<String, Object> work : list) {
            String visit_id = work.get("visit_id").toString();
            String domainName = work.get("domain_name").toString();
            VisitRequest visitRequest = new VisitRequest(visit_id, domainName);
            workQueue.add(visitRequest);
        }
        logger.debug("queueWork done, with limit={}. We added {} items on the queue",
                limit, list.size());

    }

    private void moveFile(String filename) {
        File source = new File(filename);
        try {
            FileUtils.moveFileToDirectory(source, ingestedDirectory, true);
            logger.info("Moved {} to {}", filename, ingestedDirectory);
        } catch (FileExistsException e) {
            logger.warn("Destination file already exists: {} in {}", filename, ingestedDirectory);
            File newDestination = new File(ingestedDirectory, filename + ".renamed");
            try {
                FileUtils.moveFile(source, newDestination, REPLACE_EXISTING);
            } catch (IOException ex) {
                stopIngestion(e, filename, newDestination);
            }
        } catch (IOException e) {
            stopIngestion(e, filename, ingestedDirectory);
        }
    }

    private void stopIngestion(IOException e, String filename, File destination) {
        logger.atError()
                .setMessage("Could not move file {} to {} => STOPPING INGESTION !!")
                .addArgument(filename)
                .addArgument(destination)
                .setCause(e)
                .log();
        ingestionStopped.set(true);

    }

    public void resumeIngestion() {
        ingestionStopped.set(false);
    }

    public boolean isIngestionStopped() {
        return ingestionStopped.get();
    }
}
