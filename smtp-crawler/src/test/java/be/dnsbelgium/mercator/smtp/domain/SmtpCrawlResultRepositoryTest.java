package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
import be.dnsbelgium.mercator.smtp.dto.SmtpServer;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpCrawlResult;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpCrawlResultRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
class SmtpCrawlResultRepositoryTest {

  @Autowired
  SmtpCrawlResultRepository repository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpCrawlResultRepositoryTest.class);

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void findByVisitId() {
    UUID uuid = randomUUID();
    logger.info("uuid = {}", uuid);
    SmtpCrawlResult crawlResult = new SmtpCrawlResult(uuid, "dnsbelgium.be");
    SmtpServer server1 = new SmtpServer("smtp1.example.com");
    SmtpServer server2 = new SmtpServer("smtp2.example.com");
    SmtpHostIp hostIp = new SmtpHostIp("1.2.3.4");
    hostIp.setConnectReplyCode(220);
    hostIp.setIpVersion(4);
    hostIp.setBanner("my banner");
    hostIp.setConnectionTimeMs(123);
    hostIp.setStartTlsOk(false);
    hostIp.setCountry("Jamaica");
    hostIp.setAsnOrganisation("Happy Green grass");
    hostIp.setAsn(654);
    server1.addHost(hostIp);
    crawlResult.add(server1);
    crawlResult.add(server2);
    repository.save(crawlResult);
    Optional<SmtpCrawlResult> found = repository.findFirstByVisitId(uuid);
    assertThat(found).isPresent();
    SmtpCrawlResult smtpCrawlResult = found.get();
    logger.info("found.servers = {}", smtpCrawlResult.getServers());
    assertThat(smtpCrawlResult).isNotNull();
    assertThat(smtpCrawlResult.getCrawlTimestamp()).isEqualTo(crawlResult.getCrawlTimestamp());
    assertThat(smtpCrawlResult.getDomainName()).isEqualTo(crawlResult.getDomainName());
    assertThat(smtpCrawlResult.getVisitId()).isEqualTo(crawlResult.getVisitId());
    assertThat(smtpCrawlResult.getId()).isNotNull();
    assertThat(smtpCrawlResult.getServers().size()).isEqualTo(2);
    assertThat(smtpCrawlResult.getServers()).isEqualTo(crawlResult.getServers());

    List<SmtpHostIp> hosts = smtpCrawlResult.getServers().get(0).getHosts();
    assertThat(hosts).isNotNull();
    assertThat(hosts.size()).isEqualTo(1);
    assertThat(hosts.get(0).getIp()).isEqualTo("1.2.3.4");
    assertThat(hosts.get(0).getBanner()).isEqualTo("my banner");
    assertThat(hosts.get(0)).usingRecursiveComparison().isEqualTo(hostIp);
  }

  @Test
  public void savingBinaryDataFails() {
    SmtpCrawlResult crawlResult = crawlResultWithBinaryData();
    try {
      repository.save(crawlResult);
      //noinspection ResultOfMethodCallIgnored
      fail("Binary data should throw DataIntegrityViolationException");
    } catch (DataIntegrityViolationException expected) {
      logger.info("expected = {}", expected.getMessage());
    }
  }

  @Test
  public void saveAndIgnoreDuplicateKeys() {
    UUID uuid = randomUUID();
    @SuppressWarnings("SqlResolve")
    int rowsInserted = jdbcTemplate.update("" +
        " insert into smtp_crawl_result\n" +
        "        (crawl_status, crawl_timestamp, servers, domain_name, visit_id) \n" +
        "    values\n" +
        "        (0, current_timestamp, null, ?, ?)"
      , "abc.be", uuid
    );
    logger.info("rowsInserted = {}", rowsInserted);
    List<SmtpCrawlResult> found = repository.findByVisitId(uuid);
    logger.info("found = {}", found.size());
    assertThat(found).hasSize(1);
    jdbcTemplate.execute("commit");
    SmtpCrawlResult crawlResult = crawlResult(uuid);
    boolean saveFailed = repository.saveAndIgnoreDuplicateKeys(crawlResult);
    logger.info("saveFailed = {}", saveFailed);
    assertThat(saveFailed).isTrue();
  }

  @Test
  public void otherDataIntegrityViolationExceptionNotIgnored() {
    SmtpCrawlResult crawlResult = crawlResult(randomUUID());
    crawlResult.setDomainName(StringUtils.repeat("a", 130));
    Assertions.assertThrows(
      DataIntegrityViolationException.class,
      () -> repository.saveAndIgnoreDuplicateKeys(crawlResult)
    );
  }

  @Test
  public void saveSuccessfulWhenWeCleanBinaryData() {
    SmtpCrawlResult crawlResult = crawlResultWithBinaryData();
    // clean the data before saving
    for (SmtpServer server : crawlResult.getServers()) {
      for (SmtpHostIp host : server.getHosts()) {
        host.clean();
      }
    }
    String actualCountry = crawlResult.getServers().get(0).getHosts().get(0).getCountry();
    assertThat(actualCountry).isEqualTo("Jamaica ");
    logger.info("Before save: crawlResult.getId() = {}", crawlResult.getId());
    assertThat(crawlResult.getId()).withFailMessage("Id should be null before save").isNull();
    crawlResult = repository.save(crawlResult);
    logger.info("After save: crawlResult.getId() = {}", crawlResult.getId());
    assertThat(crawlResult.getId()).isNotNull();
  }

  private SmtpCrawlResult crawlResult(UUID uuid) {
    SmtpCrawlResult crawlResult = new SmtpCrawlResult(uuid, "jamaica.be");
    SmtpServer server = new SmtpServer("smtp1.example.com");
    SmtpHostIp hostIp = new SmtpHostIp("1.2.3.4");
    hostIp.setConnectReplyCode(220);
    hostIp.setIpVersion(4);
    hostIp.setBanner("my binary banner");
    hostIp.setConnectionTimeMs(123);
    hostIp.setStartTlsOk(false);
    hostIp.setCountry("Jamaica");
    hostIp.setAsnOrganisation("Happy Green grass");
    hostIp.setAsn(654);
    server.addHost(hostIp);
    crawlResult.add(server);
    return crawlResult;
  }

  private SmtpCrawlResult crawlResultWithBinaryData() {
    UUID uuid = randomUUID();
    SmtpCrawlResult crawlResult = new SmtpCrawlResult(uuid, "dnsbelgium.be");
    SmtpServer server = new SmtpServer("smtp1.example.com");
    SmtpHostIp hostIp = new SmtpHostIp("1.2.3.4");
    hostIp.setConnectReplyCode(220);
    hostIp.setIpVersion(4);
    hostIp.setBanner("my binary \u0000 banner");
    hostIp.setConnectionTimeMs(123);
    hostIp.setStartTlsOk(false);
    hostIp.setCountry("Jamaica \u0000");
    hostIp.setAsnOrganisation("Happy \u0000 Green grass");
    hostIp.setAsn(654);
    server.addHost(hostIp);
    crawlResult.add(server);
    return crawlResult;
  }

}
