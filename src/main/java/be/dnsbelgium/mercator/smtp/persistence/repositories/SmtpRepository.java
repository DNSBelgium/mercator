package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.domain.crawler.NioSmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import com.github.f4b6a3.ulid.Ulid;
import org.duckdb.user.DuckDBUserArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

//import static be.dnsbelgium.mercator.persistence.Repository.instant;
//import static be.dnsbelgium.mercator.persistence.Repository.timestamp;

@Component
public class SmtpRepository {

//  private final DataSource dataSource;
//  private final JdbcClient jdbcClient;
//  private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);
//
//  public SmtpRepository(DataSource dataSource) {
//    this.dataSource = dataSource;
//    this.jdbcClient = JdbcClient.create(dataSource);
//  }
//
//  public void createTables() {
//    String ddl_conversation = """
//        create table if not exists smtp_conversation
//        (
//            id                   varchar          primary key,
//            ip                   varchar(64)      not null,
//            asn                  bigint,
//            country              varchar(256),
//            asn_organisation     varchar(128),
//            banner               varchar(512),
//            connect_ok           boolean,
//            connect_reply_code   integer,
//            ip_version           smallint,
//            start_tls_ok         boolean,
//            start_tls_reply_code integer,
//            error_message        varchar(256),
//            error                varchar(64),
//            connection_time_ms   bigint,
//            software             varchar(128),
//            software_version     varchar(128),
//            timestamp            timestamp,
//            supportedExtensions  varchar[]
//        )
//        """;
//    jdbcClient.sql(ddl_conversation).update();
//    logger.info("Done executing sql \n {}", ddl_conversation);
//    String smtp_host = """
//        create table if not exists smtp_host
//        (
//            id           varchar         primary key,
//            visit_id     varchar         not null,     --  logically references smtp_crawler.smtp_visit,
//            from_mx      boolean,
//            host_name    varchar(128) not null,
//            priority     integer      not null,
//            conversation varchar                       --  logically references smtp_crawler.smtp_conversation
//        )
//        """;
//    jdbcClient.sql(smtp_host).update();
//    logger.info("Done executing sql \n {}", smtp_host);
//    String smtp_visit = """
//          create table if not exists smtp_visit
//          (
//              visit_id          varchar                  not null,
//              domain_name       varchar(128)             not null,
//              timestamp         timestamp                not null,
//              num_conversations integer                  not null,
//              crawl_status      varchar(64)
//          )
//        """;
//    jdbcClient.sql(smtp_visit).update();
//    logger.info("Done executing sql \n {}", smtp_visit);
//  }
//
//  private static class SmtpConversationMapper implements RowMapper<SmtpConversation> {
//    @Override
//    public SmtpConversation mapRow(ResultSet rs, int rowNum) throws SQLException {
//      String errorMessage = rs.getString("error_message");
//      Error error = (errorMessage != null) ? NioSmtpConversation.getErrorFromErrorMessage(errorMessage) : null;
//      var extensionsArray = rs.getArray("supportedExtensions").getArray();
//      Set<String> extensions = Arrays.stream((Object[]) extensionsArray)
//          .map(Object::toString)
//          .collect(Collectors.toSet());
//      return SmtpConversation.builder()
//          .id(rs.getString("id"))
//          .ip(rs.getString("ip"))
//          .asn(rs.getLong("asn"))
//          .country(rs.getString("country"))
//          .asnOrganisation(rs.getString("asn_organisation"))
//          .banner(rs.getString("banner"))
//          .connectOK(rs.getBoolean("connect_ok"))
//          .startTlsOk(rs.getBoolean("start_tls_ok"))
//          .connectReplyCode(rs.getInt("connect_reply_code"))
//          .ipVersion(rs.getInt("ip_version"))
//          .startTlsReplyCode(rs.getInt("start_tls_reply_code"))
//          .errorMessage(errorMessage)
//          .error(error)
//          .connectionTimeMs(rs.getLong("connection_time_ms"))
//          .software(rs.getString("software"))
//          .softwareVersion(rs.getString("software_version"))
//          .timestamp(instant(rs.getTimestamp("timestamp")))
//          .supportedExtensions(extensions)
//          .build();
//    }
//  }
//
//  Optional<SmtpConversation> findConversation(String id) {
//    String query = "select * from smtp_conversation where id = :id";
//    JdbcClient jdbcClient = JdbcClient.create(dataSource);
//    return jdbcClient
//        .sql(query)
//        .param("id", id)
//        .query(new SmtpConversationMapper())
//        .optional();
//  }
//
//
//
//  public Optional<SmtpVisit> findVisit(String visitId) {
//    String query = "select * from smtp_visit where visit_id = :visit_id limit 1";
//    Optional<SmtpVisit> visit = jdbcClient
//        .sql(query)
//        .param("visit_id", visitId)
//        .query(new SmtpVisitMapper())
//        .optional();
//    if (visit.isPresent()) {
//      List<SmtpHost> hosts = jdbcClient
//          .sql("select * from smtp_host where visit_id = :visit_id")
//          .param("visit_id", visitId)
//          .query((rs, rowNum) -> SmtpHost
//              .builder()
//              .id(rs.getString("id"))
//              .fromMx(rs.getBoolean("from_mx"))
//              .hostName(rs.getString("host_name"))
//              .priority(rs.getInt("priority"))
//              .build()
//          )
//          .list();
//      for (SmtpHost host : hosts) {
//        Optional<SmtpConversation> conversation = jdbcClient
//            .sql("select smtp_conversation.* " +
//                " from smtp_conversation " +
//                " join smtp_host on smtp_host.conversation = smtp_conversation.id " +
//                " where smtp_host.id = :host_id")
//            .param("host_id", host.getId())
//            .query(new SmtpConversationMapper())
//            .optional();
//        host.setConversation(conversation.orElse(null));
//      }
//      visit.get().setHosts(hosts);
//    }
//    return visit;
//  }
//
//  public void saveVisit(SmtpVisit smtpVisit) {
//    String[] columnNames = { "visit_id", "domain_name", "timestamp", "num_conversations", "crawl_status"};
//    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
//        .withTableName("smtp_visit")
//        .withoutTableColumnMetaDataAccess()
//        .usingColumns(columnNames);
//    String crawl_status = (smtpVisit.getCrawlStatus() != null) ? smtpVisit.getCrawlStatus().name() :null;
//    SqlParameterSource parameters = new MapSqlParameterSource()
//        .addValue("visit_id", smtpVisit.getVisitId())
//        .addValue("domain_name", smtpVisit.getDomainName())
//        .addValue("timestamp", timestamp(smtpVisit.getTimestamp()))
//        .addValue("num_conversations", smtpVisit.getNumConversations())
//        .addValue("crawl_status", crawl_status);
//    insert.execute(parameters);
//    logger.info("insert into smtp_visit with params {}", parameters);
//    smtpVisit.getHosts().forEach(smtpHost -> saveHost(smtpVisit, smtpHost));
//  }
//
//  void saveHost(SmtpVisit smtpVisit, SmtpHost smtpHost) {
//    SmtpConversation conversation = smtpHost.getConversation();
//    if (conversation.getId() == null) {
//      conversation.setId(Ulid.fast().toString());
//      saveConversation(conversation);
//      logger.debug("not from cache: {}", conversation);
//    } else {
//      logger.debug("from cache: {}", conversation);
//    }
//    smtpHost.setId(Ulid.fast().toString());
//    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
//        .withTableName("smtp_host")
//        .withoutTableColumnMetaDataAccess()
//        .usingColumns("id", "visit_id", "from_mx","host_name", "priority", "conversation");
//    SqlParameterSource parameters = new MapSqlParameterSource()
//        .addValue("id", smtpHost.getId())
//        .addValue("visit_id", smtpVisit.getVisitId())
//        .addValue("from_mx", smtpHost.isFromMx())
//        .addValue("host_name", smtpHost.getHostName())
//        .addValue("priority", smtpHost.getPriority())
//        .addValue("conversation", conversation.getId());
//    logger.info("insert into smtp_host with params {}", parameters);
//    insert.execute(parameters);
//  }
//
//  void saveConversation(SmtpConversation conversation) {
//    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
//        .withTableName("smtp_conversation")
//        .withoutTableColumnMetaDataAccess()
//        .usingColumns("id", "ip", "asn","country", "asn_organisation", "banner",
//            "connect_ok", "connect_reply_code", "ip_version", "start_tls_ok", "start_tls_reply_code",
//            "error_message", "error",
//            "connection_time_ms", "software", "software_version", "timestamp", "supportedExtensions");
//    Array extensions = array(conversation.getSupportedExtensions());
//    String error = (conversation.getError() != null) ? conversation.getError().name() : null;
//    SqlParameterSource parameters = new MapSqlParameterSource()
//        .addValue("id", conversation.getId())
//        .addValue("ip", conversation.getIp())
//        .addValue("asn", conversation.getAsn())
//        .addValue("country", conversation.getCountry())
//        .addValue("asn_organisation", conversation.getAsnOrganisation())
//        .addValue("banner", conversation.getBanner())
//        .addValue("connect_ok", conversation.isConnectOK())
//        .addValue("connect_reply_code", conversation.getConnectReplyCode())
//        .addValue("ip_version", conversation.getIpVersion())
//        .addValue("start_tls_ok", conversation.isStartTlsOk())
//        .addValue("start_tls_reply_code", conversation.getStartTlsReplyCode())
//        .addValue("error_message", conversation.getErrorMessage())
//        .addValue("error", error)
//        .addValue("connection_time_ms", conversation.getConnectionTimeMs())
//        .addValue("software", conversation.getSoftware())
//        .addValue("software_version", conversation.getSoftwareVersion())
//        .addValue("timestamp", timestamp(conversation.getTimestamp()))
//        .addValue("supportedExtensions", extensions);
//    logger.info("insert into smtp_conversation with params {}", parameters);
//    insert.execute(parameters);
//  }
//
//  private Array array(Collection<String> collection) {
//    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//    if (collection == null) {
//      return new DuckDBUserArray("text", new Object[0]);
//    }
//    return jdbcTemplate.execute(
//        (ConnectionCallback<Array>) con ->
//            con.createArrayOf("text", collection.toArray())
//    );
//  }
//
//
//  private static class SmtpVisitMapper implements RowMapper<SmtpVisit> {
//    @Override
//    public SmtpVisit mapRow(ResultSet rs, int rowNum) throws SQLException {
//      String status = rs.getString("crawl_status");
//      CrawlStatus crawlStatus = (status != null) ? CrawlStatus.valueOf(status) : null;
//      return SmtpVisit
//          .builder()
//          .visitId(rs.getString("visit_id"))
//          .domainName(rs.getString("domain_name"))
//          .timestamp(instant(rs.getTimestamp("timestamp")))
//          .numConversations(rs.getInt("num_conversations"))
//          .crawlStatus(crawlStatus)
//          .build();
//    }
//  }
}
