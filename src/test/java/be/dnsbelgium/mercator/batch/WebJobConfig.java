package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;

@SuppressWarnings("SpringElInspection")
@EnableBatchProcessing
public class WebJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(CreditJobConfiguration.class);

  @Bean
  @StepScope
  public FlatFileItemReader<VisitRequest> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
    logger.info("creating FlatFileItemReader for resource {}", resource);
    return new FlatFileItemReaderBuilder<VisitRequest>().name("itemReader")
            .resource(resource)
            .delimited()
            .names("visitId", "domainName")
            .targetType(VisitRequest.class)
            .build();
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<CustomerCredit> itemWriter(
          @Value("#{jobParameters[outputFile]}") WritableResource resource) {
    return new FlatFileItemWriterBuilder<CustomerCredit>().name("itemWriter")
            .resource(resource)
            .delimited()
            .names("name", "credit")
            .build();
  }

  private static class MyProcessor implements ItemProcessor<VisitRequest, CustomerCredit> {

    @Override
    public CustomerCredit process(@NonNull VisitRequest item) throws Exception {
      return new CustomerCredit(100, "Donald Duck", BigDecimal.TEN);
    }
  }

  @Bean
  public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
                 ItemReader<VisitRequest> itemReader, ItemWriter<CustomerCredit> itemWriter) {

    Step step = new StepBuilder("step1", jobRepository)
            .<VisitRequest, CustomerCredit>chunk(100, transactionManager)
            .reader(itemReader)
            .processor(new MyProcessor())
            .writer(itemWriter)
            .build();

    //            .taskExecutor(new VirtualThreadTaskExecutor("virtual-thread"))

    return new JobBuilder("ioSampleJob", jobRepository)
            .start(step)
            .build();
  }

  @Bean
  public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
    factoryBean.setDatabaseType("POSTGRES");
    factoryBean.setDataSource(dataSource);
    factoryBean.setTransactionManager(transactionManager);
    factoryBean.afterPropertiesSet();
    return factoryBean;
  }

  @Bean
  public JobRepository jobRepository(JobRepositoryFactoryBean factoryBean) throws Exception {
    return factoryBean.getObject();
  }

  @Bean
  public DataSource dataSource() {
    DuckDataSource duckDataSource = new DuckDataSource();
    duckDataSource.setUrl("jdbc:duckdb:batch.duckdb");
    duckDataSource.init();
    return duckDataSource;
  }

  @Bean
  public JdbcTransactionManager transactionManager(DataSource dataSource) {
    return new JdbcTransactionManager(dataSource);
  }

  @Bean
  public Db db(DataSource dataSource) {
    return new Db(dataSource);
  }
}
