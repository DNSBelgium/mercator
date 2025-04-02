package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.persistence.TlsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class TlsParquetMaker implements JobExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(TlsParquetMaker.class);

  private final BatchConfig batchConfig;
  private final TlsRepository tlsRepository;

  public TlsParquetMaker(BatchConfig batchConfig, TlsRepository tlsRepository) {
    this.batchConfig = batchConfig;
    this.tlsRepository = tlsRepository;
    logger.info("TlsParquetMaker created");
  }

  public void afterJob(@NonNull JobExecution jobExecution) {
    logger.info("jobExecution = {}", jobExecution);

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {

      Path jsonFile = batchConfig.outputPathFor(jobExecution.getJobInstance().getJobName());

      if (jsonFile.toFile().exists()) {
        tlsRepository.storeResults(jsonFile.toString());
        logger.info("Saved the data in parquet format");
      } else {
        logger.error("jsonFile={} but does NOT exist", jsonFile);
        jobExecution.setExitStatus(ExitStatus.FAILED);
      }
    } else {
      logger.error("Not saving to parquet since status = {}", jobExecution.getExitStatus());
    }
  }


}
