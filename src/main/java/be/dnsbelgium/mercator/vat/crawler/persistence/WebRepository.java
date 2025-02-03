package be.dnsbelgium.mercator.vat.crawler.persistence;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.persistence.ArrayConvertor;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Array;
import java.util.List;

@Component
public class WebRepository {

//    private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);
//    private final JdbcTemplate jdbcTemplate;
//    private final JdbcClient jdbcClient;
//    private final MeterRegistry meterRegistry;
//    private final RowMapper<HtmlFeatures> htmlFeaturesRowMapper;

//    public WebRepository(DataSource dataSource, MeterRegistry meterRegistry) {
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//        this.jdbcClient = JdbcClient.create(dataSource);
//        this.meterRegistry = meterRegistry;
//      ApplicationConversionService applicationConversionService = new ApplicationConversionService();
//        applicationConversionService.addConverter(new ArrayConvertor());
//        this.htmlFeaturesRowMapper = new SimplePropertyRowMapper<>(HtmlFeatures.class, applicationConversionService);
//    }

//    public void createTables() {
//        var ddl_web_page_visit = """
//                create table if not exists web_page_visit
//                (
//                    visit_id       varchar(26)              not null,
//                    domain_name    varchar(255),
//                    crawl_started  timestamp,
//                    crawl_finished timestamp,
//                    html           text,
//                    body_text      text,
//                    status_code    integer,
//                    url            varchar(500),
//                    path           varchar(500),
//                    vat_values     varchar[],
//                    link_text      varchar(500)
//                )
//                """;
//        execute(ddl_web_page_visit);
//        var ddl_web_visit = """
//                create table if not exists web_visit
//                (
//                    visit_id       varchar(26),
//                    domain_name    varchar(255),
//                    start_url      varchar(255),
//                    matching_url   varchar(255),
//                    crawl_started  timestamp,
//                    crawl_finished timestamp,
//                    visited_urls   varchar[]
//                )
//                """;
//        execute(ddl_web_visit);
//        createTableFeatures();
//    }
//
//    public void createTableFeatures() {
//        String ddl_html_features = """
//                create table if not exists html_features
//                (
//                    visit_id                             varchar(26)              not null,
//                    crawl_timestamp                      timestamp                not null,
//                    domain_name                          varchar(128)             not null,
//                    html_length                          bigint,
//                    nb_imgs                              integer,
//                    nb_links_int                         integer,
//                    nb_links_ext                         integer,
//                    nb_links_tel                         integer,
//                    nb_links_email                       integer,
//                    nb_input_txt                         integer,
//                    nb_button                            integer,
//                    nb_meta_desc                         integer,
//                    nb_meta_keyw                         integer,
//                    nb_numerical_strings                 integer,
//                    nb_tags                              integer,
//                    nb_words                             integer,
//                    title                                varchar(2000),
//                    htmlstruct                           varchar(2000),
//                    body_text                            text,
//                    meta_text                            text,
//                    url                                  varchar(255),
//                    body_text_truncated                  boolean,
//                    meta_text_truncated                  boolean,
//                    title_truncated                      boolean,
//                    nb_letters                           integer,
//                    nb_distinct_hosts_in_urls            integer,
//                    external_hosts                       varchar[],
//                    nb_facebook_deep_links               integer,
//                    nb_facebook_shallow_links            integer,
//                    nb_linkedin_deep_links               integer,
//                    nb_linkedin_shallow_links            integer,
//                    nb_twitter_deep_links                integer,
//                    nb_twitter_shallow_links             integer,
//                    nb_currency_names                    integer,
//                    nb_distinct_currencies               integer,
//                    distance_title_final_dn              integer,
//                    longest_subsequence_title_final_dn   integer,
//                    facebook_links                       varchar[],
//                    twitter_links                        varchar[],
//                    linkedin_links                       varchar[],
//                    youtube_links                        varchar[],
//                    vimeo_links                          varchar[],
//                    nb_youtube_deep_links                integer,
//                    nb_youtube_shallow_links             integer,
//                    nb_vimeo_deep_links                  integer,
//                    nb_vimeo_shallow_links               integer,
//                    body_text_language                   varchar(128),
//                    body_text_language_2                 varchar(128),
//                    fraction_words_title_initial_dn      double precision,
//                    fraction_words_title_final_dn        double precision,
//                    nb_distinct_words_in_title           integer,
//                    distance_title_initial_dn            integer,
//                    longest_subsequence_title_initial_dn integer,
//                )
//                """;
//        execute(ddl_html_features);
//        // this table is/was used when trying to find out why some inserts into html_features take over 500ms
//        jdbcClient
//                .sql("create table if not exists latency" +
//                        "(timestamp timestamp, table_name varchar, id varchar, url varchar, ms int)")
//                .update();
//    }
//
//    private void execute(String sql) {
//        logger.debug("Start executing sql = {}", sql);
//        jdbcTemplate.execute(sql);
//        logger.debug("Done executing sql {}", sql);
//    }

//    public void savePageVisits(List<PageVisit> pageVisits) {
//        for (PageVisit pageVisit : pageVisits) {
//            save(pageVisit);
//        }
//    }

//    public void save(@NotNull PageVisit pageVisit) {
//        logger.debug("Saving PageVisit with url={}", pageVisit.getUrl());
//        var sample = Timer.start(meterRegistry);
//        String insert = """
//        insert into web_page_visit(
//          visit_id,  domain_name,  crawl_started,  crawl_finished,  html,  body_text,  status_code,  url,  path,  vat_values,  link_text)
//        values (
//          :visit_id, :domain_name, :crawl_started, :crawl_finished, :html, :body_text, :status_code, :url, :path, :vat_values, :link_text)
//        """;
//        jdbcClient
//                .sql(insert)
//                .param("visit_id", pageVisit.getVisitId())
//                .param("domain_name", pageVisit.getDomainName())
//                .param("crawl_started", timestamp(pageVisit.getCrawlStarted()))
//                .param("crawl_finished", timestamp(pageVisit.getCrawlFinished()))
//                .param("html", pageVisit.getHtml())
//                .param("body_text", pageVisit.getBodyText())
//                .param("status_code", pageVisit.getStatusCode())
//                .param("url", pageVisit.getUrl())
//                .param("path", pageVisit.getPath())
//                .param("link_text", pageVisit.getLinkText())
//                .param("vat_values", array(pageVisit.getVatValues()))
//                .update();
//        sample.stop(meterRegistry.timer("repository.insert.web.page.visit"));
//    }

//    private Array array(List<String> list) {
//        return jdbcTemplate.execute(
//                (ConnectionCallback<Array>) con ->
//                        con.createArrayOf("text", list.toArray())
//        );
//    }

//    public void saveWebVisit(@NotNull WebCrawlResult crawlResult) {
//        var sample = Timer.start(meterRegistry);
//        var insert = """
//            insert into web_visit
//            (
//                visit_id,
//                domain_name,
//                start_url,
//                matching_url,
//                crawl_started,
//                crawl_finished,
//                visited_urls
//            )
//            values (?, ?, ?, ?, ?, ?, ?)
//            """;
//        Array visitedUrls = jdbcTemplate.execute(
//                (ConnectionCallback<Array>) con ->
//                        con.createArrayOf("text", crawlResult.getVisitedUrls().toArray())
//        );
//        int rowsInserted = jdbcTemplate.update(
//                insert,
//                crawlResult.getVisitId(),
//                crawlResult.getDomainName(),
//                crawlResult.getStartUrl(),
//                crawlResult.getMatchingUrl(),
//                timestamp(crawlResult.getCrawlStarted()),
//                timestamp(crawlResult.getCrawlFinished()),
//                visitedUrls
//        );
//        logger.debug("domain={} rowsInserted={}", crawlResult.getDomainName(), rowsInserted);
//        sample.stop(meterRegistry.timer("repository.insert.web.visit"));
//    }
//
//    public List<PageVisit> findPageVisits(String visitId) {
//        List<PageVisit> found = jdbcClient
//                .sql("select * from web_page_visit where visit_id = ?")
//                .param(visitId)
//                .query((rs, rowNum) -> {
//                    Instant crawl_started = instant(rs.getTimestamp("crawl_started"));
//                    Instant crawl_finished = instant(rs.getTimestamp("crawl_finished"));
//
//                    List<String> vat_values = getList(rs, "vat_values");
//
//                    return PageVisit
//                            .builder()
//                            .visitId(visitId)
//                            .domainName(rs.getString("domain_name"))
//                            .crawlStarted(crawl_started)
//                            .crawlFinished(crawl_finished)
//                            .html(rs.getString("html"))
//                            .bodyText(rs.getString("body_text"))
//                            .statusCode(rs.getInt("status_code"))
//                            .url(rs.getString("url"))
//                            .path(rs.getString("path"))
//                            .vatValues(vat_values)
//                            .linkText(rs.getString("link_text"))
//                            .build();
//                })
//                .list();
//        logger.debug("findPageVisits {} => found {} pages", visitId, found.size());
//        return found;
//    }
//
//    public List<HtmlFeatures> findHtmlFeatures(String visitId) {
//        return jdbcClient
//                .sql("select * from html_features where visit_id = ?")
//                .param(visitId)
//                .query(htmlFeaturesRowMapper)
//                .list();
//    }
//
//    public List<WebCrawlResult> findWebCrawlResult(String visitId) {
//        return jdbcClient
//                .sql("select * from web_visit where visit_id = ?")
//                .param(visitId)
//                .query((rs, rowNum) -> {
//                    String domainName = rs.getString("domain_name");
//                    String startUrl = rs.getString("start_url");
//                    String matchingUrl = rs.getString("matching_url");
//                    Instant crawl_started = instant(rs.getTimestamp("crawl_started"));
//                    Instant crawl_finished = instant(rs.getTimestamp("crawl_finished"));
//                    List<String> visitedUrls = getList(rs, "visited_urls");
//                    logger.info("visitedUrls = {}", visitedUrls);
//                    for (String visitedUrl : visitedUrls) {
//                        logger.info("visitedUrl = [{}]", visitedUrl);
//                    }
//                    return WebCrawlResult
//                            .builder()
//                            .visitId(visitId)
//                            .domainName(domainName)
//                            .startUrl(startUrl)
//                            .crawlStarted(crawl_started)
//                            .crawlFinished(crawl_finished)
//                            .matchingUrl(matchingUrl)
//                            .visitedUrls(visitedUrls)
//                            .build();
//                }).
//                list();
//    }

//    public void save(@NotNull HtmlFeatures h) {
//        // we avoid the @Timed aspect since it generates ugly stack traces ...
//        var sample = Timer.start(meterRegistry);
//        long start = System.currentTimeMillis();
//        var insert = """
//            insert into html_features(
//                visit_id,
//                crawl_timestamp,
//                domain_name,
//                html_length,
//                nb_imgs,
//                nb_links_int,
//                nb_links_ext,
//                nb_links_tel,
//                nb_links_email,
//                nb_input_txt,
//                nb_button,
//                nb_meta_desc,
//                nb_meta_keyw,
//                nb_numerical_strings,
//                nb_tags,
//                nb_words,
//                title,
//                htmlstruct,
//                body_text,
//                meta_text,
//                url,
//                body_text_truncated,
//                meta_text_truncated,
//                title_truncated,
//                nb_letters,
//                nb_distinct_hosts_in_urls,
//                external_hosts,
//                nb_facebook_deep_links    ,
//                nb_facebook_shallow_links ,
//                nb_linkedin_deep_links    ,
//                nb_linkedin_shallow_links ,
//                nb_twitter_deep_links     ,
//                nb_twitter_shallow_links  ,
//                nb_currency_names         ,
//                nb_distinct_currencies    ,
//                distance_title_final_dn   ,
//                longest_subsequence_title_final_dn,
//                facebook_links            ,
//                twitter_links             ,
//                linkedin_links            ,
//                youtube_links             ,
//                vimeo_links               ,
//                nb_youtube_deep_links     ,
//                nb_youtube_shallow_links  ,
//                nb_vimeo_deep_links       ,
//                nb_vimeo_shallow_links    ,
//                body_text_language        ,
//                body_text_language_2      ,
//                fraction_words_title_initial_dn,
//                fraction_words_title_final_dn  ,
//                nb_distinct_words_in_title,
//                distance_title_initial_dn ,
//                longest_subsequence_title_initial_dn
//                )"""
//                + values(53);
//        jdbcTemplate.update(insert,
//                h.visitId,
//                timestamp(h.crawlTimestamp),
//                h.domainName,
//                h.html_length,
//                h.nb_imgs,
//                h.nb_links_int,
//                h.nb_links_ext,
//                h.nb_links_tel,
//                h.nb_links_email,
//                h.nb_input_txt,
//                h.nb_button,
//                h.nb_meta_desc,
//                h.nb_meta_keyw,
//                h.nb_numerical_strings,
//                h.nb_tags,
//                h.nb_words,
//                h.title,
//                h.htmlstruct,
//                h.body_text,
//                h.meta_text,
//                h.url,
//                h.body_text_truncated,
//                h.meta_text_truncated,
//                h.title_truncated,
//                h.nb_letters,
//                h.nb_distinct_hosts_in_urls,
//                array(h.external_hosts),
//                h.nb_facebook_deep_links,
//                h.nb_facebook_shallow_links,
//                h.nb_linkedin_deep_links,
//                h.nb_linkedin_shallow_links,
//                h.nb_twitter_deep_links,
//                h.nb_twitter_shallow_links,
//                h.nb_currency_names,
//                h.nb_distinct_currencies,
//                h.distance_title_final_dn,
//                h.longest_subsequence_title_final_dn,
//                array(h.facebook_links),
//                array(h.twitter_links),
//                array(h.linkedin_links),
//                array(h.youtube_links),
//                array(h.vimeo_links),
//                h.nb_youtube_deep_links,
//                h.nb_youtube_shallow_links,
//                h.nb_vimeo_deep_links,
//                h.nb_vimeo_shallow_links,
//                h.body_text_language,
//                h.body_text_language_2,
//                h.fraction_words_title_initial_dn,
//                h.fraction_words_title_final_dn,
//                h.nb_distinct_words_in_title,
//                h.distance_title_initial_dn,
//                h.longest_subsequence_title_initial_dn
//        );
//        sample.stop(meterRegistry.timer("repository.save.html.features"));
//        long millis = System.currentTimeMillis() - start;
//        jdbcClient.sql(
//                        "insert into latency(timestamp, table_name, id, url, ms) " +
//                                "values (current_timestamp, 'html_features', :id, :url, :ms)")
//                .param("id", h.visitId)
//                .param("ms", millis)
//                .param("url", h.url)
//                .update();
//    }

}
