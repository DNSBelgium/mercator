package be.dnsbelgium.mercator.batch;

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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class JobConfiguration {

  //@Bean
//  public DataSource dataSource() {
//    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
//    return builder
//            //.setType(EmbeddedDatabaseType.H2)
//            .addScript("classpath:org/springframework/batch/core/schema-drop-h2.sql")
//            .addScript("classpath:org/springframework/batch/core/schema-h2.sql")
//            .build();
//  }

//  @Bean
//  public JdbcTransactionManager transactionManager(DataSource dataSource) {
//    return new JdbcTransactionManager(dataSource);
//  }



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
  public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
                 ItemReader<CustomerCredit> itemReader, ItemWriter<CustomerCredit> itemWriter) {

    Step step = new StepBuilder("step1", jobRepository)
            .<CustomerCredit, CustomerCredit>chunk(2, transactionManager)
            .reader(itemReader)
            .processor(new CustomerCreditIncreaseProcessor())
            .writer(itemWriter)
            .build();
    return new JobBuilder("ioSampleJob", jobRepository)
            .start(step)
            .build();
  }

}
