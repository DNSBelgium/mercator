package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.testcontainers.junit.jupiter.Container;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.transaction.Transactional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"local", "test"})
@Disabled
public class CacheIntegrationTest {

  @Autowired
  TlsCrawler tlsCrawler;

  @Autowired
  TlsCrawlerService tlsCrawlerService;

  //@Autowired
  //SessionFactory sessionFactory;

//  @Autowired
//  EntityManager entityManager;

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "tls_crawler");
  }

  private static final Logger logger = getLogger(CacheIntegrationTest.class);

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired JdbcTemplate jdbcTemplate;

  @SuppressWarnings("SqlDialectInspection")
  @BeforeTransaction
  @BeforeEach
  @Transactional
  public void before() {
//    int rows = jdbcTemplate.update(
//        "insert into  tls_crawler.tls_scan_result (visit_id, domain_name, crawl_timestamp, scan_result, host_name_matches_certificate, host_name)\n" +
//        "values ('e6ceecb5-b777-4866-9c16-c6157e87f6c4', 'allesmoetduurzaam.be', '2022-07-22 14:44:49.552215 +00:00', null, false, 'allesmoetduurzaam.be')");
//
//    logger.info("rows = {}", rows);
//    jdbcTemplate.execute("commit");
  }


  @Test
  public void visitTwoDomains() {
    //server_name
    //allesmoetduurzaam.be
    //sportgolf.be
    UUID visitId = UUID.fromString("e6ceecb5-b777-4866-9c16-c6157e87f6c4");

    VisitRequest visitRequest1 = new VisitRequest(visitId, "allesmoetduurzaam.be");
    VisitRequest visitRequest2 = new VisitRequest(UUID.randomUUID(), "sportgolf.be");
    logger.info("======= visiting allesmoetduurzaam.be ====");
    try {
      // this will generate an SQL exception
      CrawlResult result = tlsCrawlerService.visit(visitRequest1);
      tlsCrawlerService.persist(result);

    } catch (DataIntegrityViolationException e) {
      logger.info("Expected: {}", e.getMessage());
    }

    logger.info("======= visiting sportgolf.be ====");
    CrawlResult result2 = tlsCrawlerService.visit(visitRequest2);
    tlsCrawlerService.persist(result2);

    // javax.cache.CacheManager cacheManager;

//    int size = CacheManager.
//    .get(0)
//        .getCache("com.baeldung.hibernate.cache.model.Foo").getSize();

    CachingProvider provider = Caching.getCachingProvider();
    CacheManager cacheManager = provider.getCacheManager();

    for (String cacheName : cacheManager.getCacheNames()) {
      logger.info("cacheName = {}", cacheName);
      logger.info("   cache = {}", cacheManager.getCache(cacheName));
      Cache<Object, Object> cache = cacheManager.getCache(cacheName);
      logger.info("cache = {}", cache);
      logger.info("cache = {}", cache.getClass());

      MutableConfiguration<Long, String> configuration;


      // Eh107Cache
//      if (cache instanceof Eh107Cache eh107Cache) {
//        eh107Cache.
//            // cache.getStatisticsMBean();
//      }

    }

    SessionFactory sessionFactory;

    //Statistics statistics = session.getSessionFactory().getStatistics();
//    CacheRegionStatistics secondLevelCacheStatistics =
//        statistics.getDomainDataRegionStatistics( "query.cache.person" );
//    long hitCount = secondLevelCacheStatistics.getHitCount();
//    long missCount = secondLevelCacheStatistics.getMissCount();
//    double hitRatio = (double) hitCount / ( hitCount + missCount );

    /*
2022-07-22 14:46:42.747  INFO 68919 --- [           main] b.d.m.tls.domain.TlsCrawlerService       : certificate = 67add1166b020ae61b8f5fc96813c04c2aa589960796865572a3c7e737613dfd
Hibernate: select certificat0_.sha256_fingerprint as sha1_1_0_, certificat0_.issuer as issuer2_1_0_, certificat0_.not_after as not_afte3_1_0_, certificat0_.not_before as not_befo4_1_0_, certificat0_.public_key_length as public_k5_1_0_, certificat0_.public_key_schema as public_k6_1_0_, certificat0_.serial_number as serial_n7_1_0_, certificat0_.signature_hash_algorithm as signatur8_1_0_, certificat0_.signed_by_sha256 as signed_b9_1_0_, certificat0_.subject as subject10_1_0_, certificat0_.subject_alt_names as subject11_1_0_, certificat0_.version as version12_1_0_ from certificate certificat0_ where certificat0_.sha256_fingerprint=?

2022-07-22 14:46:42.789  INFO 68919 --- [           main] b.d.m.tls.domain.TlsCrawlerService       : We saved certificate with fingerprint 67add1166b020ae61b8f5fc96813c04c2aa589960796865572a3c7e737613dfd
2022-07-22 14:46:42.789  INFO 68919 --- [           main] b.d.m.tls.domain.TlsCrawlerService       : certificate = 58fa7c14e6b36350c62a5915addf2aa02b3439ee9ed40b43dfd47e840cbc9b21
Hibernate: select certificat0_.sha256_fingerprint as sha1_1_0_, certificat0_.issuer as issuer2_1_0_, certificat0_.not_after as not_afte3_1_0_, certificat0_.not_before as not_befo4_1_0_, certificat0_.public_key_length as public_k5_1_0_, certificat0_.public_key_schema as public_k6_1_0_, certificat0_.serial_number as serial_n7_1_0_, certificat0_.signature_hash_algorithm as signatur8_1_0_, certificat0_.signed_by_sha256 as signed_b9_1_0_, certificat0_.subject as subject10_1_0_, certificat0_.subject_alt_names as subject11_1_0_, certificat0_.version as version12_1_0_ from certificate certificat0_ where certificat0_.sha256_fingerprint=?
2022-07-22 14:46:42.795  INFO 68919 --- [           main] b.d.m.tls.domain.TlsCrawlerService       : We saved certificate with fingerprint 58fa7c14e6b36350c62a5915addf2aa02b3439ee9ed40b43dfd47e840cbc9b21

Hibernate: insert into certificate (issuer, not_after, not_before, public_key_length, public_key_schema, serial_number, signature_hash_algorithm, signed_by_sha256, subject, subject_alt_names, version, sha256_fingerprint) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
Hibernate: insert into certificate (issuer, not_after, not_before, public_key_length, public_key_schema, serial_number, signature_hash_algorithm, signed_by_sha256, subject, subject_alt_names, version, sha256_fingerprint) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

=> for each certificate:  select + insert

     */
  }
}
