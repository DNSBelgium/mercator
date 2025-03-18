package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.persistence.WebRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class WebParquetMaker implements JobExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(WebParquetMaker.class);

  private final BatchConfig batchConfig;
  private final WebRepository webRepository;

  public WebParquetMaker(BatchConfig batchConfig, WebRepository webRepository) {
    this.batchConfig = batchConfig;
    this.webRepository = webRepository;
    logger.info("WebParquetMaker created");
  }

  public void afterJob(@NonNull JobExecution jobExecution) {
    logger.info("jobExecution = {}", jobExecution);

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {

      Path jsonFile = batchConfig.outputPathFor(jobExecution.getJobInstance().getJobName());

      if (jsonFile.toFile().exists()) {
        webRepository.toParquet(jsonFile);
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
