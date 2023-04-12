package be.dnsbelgium.mercator.smtp.domain;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
import be.dnsbelgium.mercator.smtp.dto.SmtpServer;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpCrawlResult;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpServerEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpCrawlResultRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpHostRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
public class SmtpHostRepositoryTest {
  @Autowired
  SmtpHostRepository repository;
  @Autowired
  SmtpCrawlResultRepository crawlResultRepository;
  @Autowired
  ApplicationContext context;
  @Autowired
  JdbcTemplate jdbcTemplate;

  private static final Logger logger = getLogger(SmtpHostRepositoryTest.class);

  @Container
  static PostgreSqlContainer container = PostgreSqlContainer.getInstance();

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    container.setDatasourceProperties(registry, "smtp_crawler");
  }

  @Test
  void findRecentCrawlByIp() {
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
    crawlResultRepository.save(crawlResult);
    Optional<SmtpHostEntity> hostEntity = repository.findRecentCrawlByIp("1.2.3.4", ZonedDateTime.now());
    assertThat(hostEntity.isPresent()).isEqualTo(true);
    SmtpHostEntity entity = hostEntity.get();
    assertThat(entity.getIp()).isEqualTo("1.2.3.4");
    assertThat(entity.getBanner()).isEqualTo("my banner");
    List<SmtpCrawlResult> servers = new java.util.ArrayList<>(entity.getServers().stream().map(SmtpServerEntity::getCrawlResult).toList());
    servers.sort(Comparator.comparing(SmtpCrawlResult::getCrawlTimestamp));
    ZonedDateTime zonedDateTime = servers.get(0).getCrawlTimestamp();
    assertThat(zonedDateTime).isAfter(ZonedDateTime.now().minusDays(1));
  }

  @Test
  void findRecentCrawlByIpFail() {
    UUID uuid = randomUUID();
    logger.info("uuid = {}", uuid);
    SmtpCrawlResult crawlResult = new SmtpCrawlResult(uuid, "dnsbelgium.be");
    crawlResult.setCrawlTimestamp(ZonedDateTime.of(2023, 3, 10, 13, 50, 0, 0, ZoneId.of("Europe/Brussels")));
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
    crawlResultRepository.save(crawlResult);
    Optional<SmtpHostEntity> hostEntity = repository.findRecentCrawlByIp("1.2.3.4", ZonedDateTime.now());
    assertThat(hostEntity.isPresent()).isEqualTo(false);
  }
}
