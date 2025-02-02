package be.dnsbelgium.mercator.batch;

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
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@SuppressWarnings("unused")
@EnableBatchProcessing
public class CreditJobConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(CreditJobConfiguration.class);

//  private static class MyWriter implements ItemWriter<CustomerCredit> {
//
//    @Override
//    public void write(Chunk<? extends CustomerCredit> chunk) {
//      logger.info("Writing a chunk of size {}", chunk.size());
//    }
//  }
//
//  private static class MyLongWriter implements ItemWriter<Long> {
//
//    @Override
//    public void write(Chunk<? extends Long> chunk) {
//      logger.info("MyLongWriter writing a chunk of size {}", chunk.size());
//      for (Long item : chunk.getItems()) {
//        logger.info("item = {}", item);
//      }
//    }
//  }
//
//  private record MyProcessor(String name) implements ItemProcessor<CustomerCredit, Long> {
//
//    private static final Logger logger = LoggerFactory.getLogger(MyProcessor.class);
//
//    @Override
//    public Long process(@NonNull CustomerCredit item) {
//      logger.info("{}: processing item = {}", name, item);
//      return item.getCredit().longValue();
//    }
//  }

  @SuppressWarnings("SpringElInspection")
  @Bean
  @StepScope
  public FlatFileItemReader<CustomerCredit> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
    return new FlatFileItemReaderBuilder<CustomerCredit>().name("itemReader")
            .resource(resource)
            .delimited()
            .names("name", "credit")
            .targetType(CustomerCredit.class)
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

  @Bean
  public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
                 ItemReader<CustomerCredit> itemReader, ItemWriter<CustomerCredit> itemWriter) {

    Step step = new StepBuilder("step1", jobRepository)
            .<CustomerCredit, CustomerCredit>chunk(2, transactionManager)
            .reader(itemReader)
            .processor(new CustomerCreditIncreaseProcessor())
            .writer(itemWriter)
            // there is only one writer, last one set is used
            //.writer(new MyWriter())
            .build();

//    Step step2 = new StepBuilder("step2", jobRepository)
//            .<CustomerCredit, Long>chunk(2, transactionManager)
//            .reader(itemReader)
//            //.processor(new MyProcessor("one"))
//            .taskExecutor(new VirtualThreadTaskExecutor("virtual-thread"))
//            .processor(new MyProcessor("two"))
//            .writer(new MyLongWriter())
//            .build();

//    Step step3 = new StepBuilder("web", jobRepository)
//            .chunk(10, transactionManager)
//            .build();

    return new JobBuilder("ioSampleJob", jobRepository)
            .start(step)
//            .next(step2)
//            .next(step3)
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
