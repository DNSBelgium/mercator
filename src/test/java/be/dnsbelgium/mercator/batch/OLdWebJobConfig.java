package be.dnsbelgium.mercator.batch;

//@SuppressWarnings("SpringElInspection")
//@EnableBatchProcessing
public class OLdWebJobConfig {

//  @Bean
//  @StepScope
//  public FlatFileItemReader<VisitRequest> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
//    logger.info("creating FlatFileItemReader for resource {}", resource);
//    return new FlatFileItemReaderBuilder<VisitRequest>().name("itemReader")
//            .resource(resource)
//            .delimited()
//            .names("visitId", "domainName")
//            .targetType(VisitRequest.class)
//            .build();
//  }

//
//  @Bean
//  public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
//                 ItemReader<VisitRequest> itemReader, ItemWriter<CustomerCredit> itemWriter) {
//
//    Step step = new StepBuilder("step1", jobRepository)
//            .<VisitRequest, CustomerCredit>chunk(100, transactionManager)
//            .reader(itemReader)
//            .processor(new MyProcessor())
//            .writer(itemWriter)
//            .build();
//
//    //            .taskExecutor(new VirtualThreadTaskExecutor("virtual-thread"))
//
//    return new JobBuilder("ioSampleJob", jobRepository)
//            .start(step)
//            .build();
//  }

//  @Bean
//  public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
//    JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
//    factoryBean.setDatabaseType("POSTGRES");
//    factoryBean.setDataSource(dataSource);
//    factoryBean.setTransactionManager(transactionManager);
//    factoryBean.afterPropertiesSet();
//    return factoryBean;
//  }
//
//  @Bean
//  public JobRepository jobRepository(JobRepositoryFactoryBean factoryBean) throws Exception {
//    return factoryBean.getObject();
//  }
//
//  @Bean
//  public DataSource dataSource() {
//    DuckDataSource duckDataSource = new DuckDataSource();
//    duckDataSource.setUrl("jdbc:duckdb:batch.duckdb");
//    duckDataSource.init();
//    return duckDataSource;
//  }

//  @Bean
//  public JdbcTransactionManager transactionManager(DataSource dataSource) {
//    return new JdbcTransactionManager(dataSource);
//  }

//  @Bean
//  public Db db(DataSource dataSource) {
//    return new Db(dataSource);
//  }
}
