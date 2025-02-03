package be.dnsbelgium.mercator.persistence;

import org.springframework.stereotype.Component;

@SuppressWarnings("SqlDialectInspection")
@Component
public class TableCreator {

//    private final JdbcTemplate template;
//    private static final Logger logger = LoggerFactory.getLogger(TableCreator.class);
//
//    private final SmtpCrawler smtpCrawler;
//    private final TlsCrawler tlsCrawler;
//    private final WebRepository webRepository;
//
//    @Autowired
//    public TableCreator(DuckDataSource dataSource, SmtpCrawler smtpCrawler, TlsCrawler tlsCrawler, WebRepository webRepository) {
//        this.template = new JdbcTemplate(dataSource);
//        this.smtpCrawler = smtpCrawler;
//        this.tlsCrawler = tlsCrawler;
//        this.webRepository = webRepository;
//    }
//
//    @PostConstruct
//    public void init() {
//        createWorkTables();
//    }
//
//    public void createVisitTables() {
//        createSequence();
//        createTablesDns();
//        blacklistEntry();
//        // ugly null checks until we have refactored all crawlers to CrawlerModule
//        if (webRepository != null) {
//            webRepository.createTables();
//        }
//        if (smtpCrawler != null) {
//            smtpCrawler.createTables();
//        }
//        if (tlsCrawler != null) {
//            tlsCrawler.createTables();
//        }
//    }
//
//    private void createWorkTables() {
//        createSequence();
//        var ddl_work = "create table if not exists work (visit_id varchar, domain_name varchar)";
//        execute(ddl_work);
//        var ddl_done = "create table if not exists done (visit_id varchar, domain_name varchar, done timestamp)";
//        execute(ddl_done);
//        var ddl_ingested = """
//                create table if not exists ingested (
//                    ingestion_id    bigint,
//                    visit_id        varchar,
//                    domain_name     varchar,
//                    filename        varchar,
//                    ingested_at     timestamp
//                )""";
//        execute(ddl_ingested);
//
//        var ddl_operations = """
//                create table if not exists operations (
//                    ts      timestamp,
//                    sql     varchar,
//                    millis  bigint
//                )
//                """;
//        execute(ddl_operations);
//    }
//
//    private void blacklistEntry() {
//        var ddl_blacklist = """
//            create table if not exists blacklist_entry(
//                cidr_prefix varchar(256)    primary key
//            )
//            """;
//        execute(ddl_blacklist);
//    }
//
//    private void execute(String sql) {
//        logger.info("Start executing sql = {}", sql);
//        template.execute(sql);
//        logger.info("Done executing sql {}", sql);
//    }
//
//    public void createSequence() {
//        execute("CREATE SEQUENCE if not exists serial START 1");
//    }
//
//    public void createTablesDns() {
//        var ddl_request = """
//                create table if not exists dns_request
//                (
//                    id               varchar(26)              primary key,
//                    visit_id         varchar(26)              not null,
//                    domain_name      varchar(128)             not null,
//                    prefix           varchar(63)              not null,
//                    record_type      char(10)                 not null,
//                    rcode            integer,
//                    crawl_timestamp  timestamp                not null,
//                    ok               boolean,
//                    problem          text,
//                    num_of_responses integer                  not null
//                )
//                """;
//        execute(ddl_request);
//        var ddl_response = """
//                create table if not exists dns_response
//                (
//                    id                  varchar         primary key,
//                    dns_request         varchar         not null,
//                    record_data         text            not null,
//                    ttl                 integer
//                )
//                """;
//        execute(ddl_response);
//        var geo = """
//                create table if not exists response_geo_ips
//                (
//                    dns_response     varchar,
//                    asn              varchar(255),
//                    country          varchar(255),
//                    ip               varchar(255),
//                    asn_organisation varchar(128),
//                    ip_version       integer not null
//                )
//                """;
//        execute(geo);
//    }

}
