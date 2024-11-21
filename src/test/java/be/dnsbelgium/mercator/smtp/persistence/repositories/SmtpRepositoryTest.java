package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class SmtpRepositoryTest {

  private final DuckDataSource dataSource = DuckDataSource.memory();
  private final JdbcClient jdbcClient = JdbcClient.create(dataSource);
  private final SmtpRepository smtpRepository = new SmtpRepository(dataSource);
  private static final Logger logger = LoggerFactory.getLogger(SmtpRepositoryTest.class);

  @BeforeEach
  public void init() {
    smtpRepository.createTables();
  }

  @Test
  public void createTables() {
    List<String> tableNames = jdbcClient.sql("show tables").query(String.class).list();
    System.out.println("tableNames = " + tableNames);
    assertThat(tableNames).contains("smtp_conversation", "smtp_host", "smtp_visit");
  }

  @Test
  public void saveAndFindVisit() {
    smtpRepository.createTables();
    SmtpVisit visit = visit();
    smtpRepository.saveVisit(visit);
    Optional<SmtpVisit> found = smtpRepository.findVisit(visit.getVisitId());
    System.out.println("found = " + found);
    assertThat(found.isPresent()).isTrue();
    assertThat(found.get().getVisitId()).isEqualTo(visit.getVisitId());
    assertThat(found.get().getCrawlStatus()).isEqualTo(visit.getCrawlStatus());
    assertThat(found.get().getHosts()).isEqualTo(visit.getHosts());
    assertThat(found.get()).isEqualTo(visit);
  }

  @Test
  public void equals() {
    SmtpHost host1 = host("abcd", "convo-1");
    SmtpHost host2 = host("abcd", "convo-1");

    Instant now = Instant.now();
    host1.getConversation().setTimestamp(now);
    host2.getConversation().setTimestamp(now);

    assertThat(host1.getConversation()).isEqualTo(host2.getConversation());
    assertThat(host1).isEqualTo(host2);
  }


  @Test
  public void saveHost() {
    smtpRepository.createTables();
    var smtpConversation = conversation("101");
    SmtpHost host = new SmtpHost();
    SmtpVisit smtpVisit = visit();
    host.setHostName("smtp.example.com");
    host.setConversation(smtpConversation);
    host.setPriority(10);
    host.setFromMx(false);
    host.setId("105");
    smtpRepository.saveHost(smtpVisit, host);
    var rows = jdbcClient.sql("from smtp_host").query().listOfRows();
    rows.forEach(System.out::println);
  }

  @Test
  public void saveConversation() {
    smtpRepository.createTables();
    String id = RandomStringUtils.randomAscii(10);
    var smtpConversation = conversation(id);
    Instant instant = smtpConversation.getTimestamp();
    smtpRepository.saveConversation(smtpConversation);
    List<Map<String, Object>> rows = jdbcClient.sql("select * from smtp_conversation").query().listOfRows();
    System.out.println("rows = " + rows);
    Optional<SmtpConversation> found = smtpRepository.findConversation(id);
    System.out.println("found = " + found);
    assertThat(found.isPresent()).isTrue();
    assertThat(found.get().getId()).isEqualTo(id);
    assertThat(found.get().getIp()).isEqualTo("127.0.0.1");
    assertThat(found.get().getAsn()).isEqualTo(14506L);
    assertThat(found.get().getAsnOrganisation()).isEqualTo("Acme Corp.");
    assertThat(found.get().getCountry()).isEqualTo("BE");
    assertThat(found.get().getBanner()).isEqualTo("Welcome to Acme SMTP server");
    assertThat(found.get().getSoftware()).isEqualTo("ACME SMTP");
    assertThat(found.get().getSoftwareVersion()).isEqualTo("0.never");
    assertThat(found.get().getTimestamp()).isEqualTo(instant);
    assertThat(found.get().getErrorMessage()).isEqualTo("Connection timed out");
    assertThat(found.get().isConnectOK()).isEqualTo(true);
    assertThat(found.get().isStartTlsOk()).isEqualTo(true);
    assertThat(found.get().getConnectReplyCode()).isEqualTo(220);
    assertThat(found.get().getStartTlsReplyCode()).isEqualTo(230);
    assertThat(found.get().getConnectionTimeMs()).isEqualTo(123L);
    assertThat(found.get().getIpVersion()).isEqualTo(4);
    assertThat(found.get().getSupportedExtensions()).isEqualTo(smtpConversation.getSupportedExtensions());
    assertThat(found.get()).isEqualTo(smtpConversation);
  }

  @Test
  public void savingBinaryDataWorks() {
    SmtpVisit visit = smtpVisitWithBinaryData();
    smtpRepository.saveVisit(visit);
    Optional<SmtpVisit> found = smtpRepository.findVisit(visit.getVisitId());
    assertThat(found.isPresent()).isTrue();
    String jvm = System.getProperty("java.version");
    System.out.println("jvm = " + jvm);
    System.out.println("found.get().getTimestamp() = " + found.get().getTimestamp());
    System.out.println("found.seconds = " + found.get().getTimestamp().getEpochSecond());

    assertThat(found.get().getTimestamp().getEpochSecond()).isEqualTo(visit.getTimestamp().getEpochSecond());
    assertThat(found.get().getTimestamp().getNano()).isEqualTo(visit.getTimestamp().getNano());

    System.out.println("found.nano = " + found.get().getTimestamp().getNano());
    System.out.println("visit.seconds = " + visit.getTimestamp().getEpochSecond());
    System.out.println("visit.nano = " + visit.getTimestamp().getNano());
    assertThat(found.get()).isEqualTo(visit);
  }

  @Test
  public void saveSuccessfulWhenWeCleanBinaryData() {
    SmtpVisit smtpVisit = smtpVisitWithBinaryData();
    // clean the data before saving
    for (SmtpHost host : smtpVisit.getHosts()) {
      host.getConversation().clean();
    }
    String actualCountry = smtpVisit.getHosts().get(0).getConversation().getCountry();
    assertThat(actualCountry).isEqualTo("Jamaica ");
    logger.info("Before save: smtpVisit.getVisitId() = {}", smtpVisit.getVisitId());
    smtpRepository.saveVisit(smtpVisit);
    logger.info("After save: crawlResult.getVisitId() = {}", smtpVisit.getVisitId());
    assertThat(smtpVisit.getVisitId()).isNotNull();
  }

}