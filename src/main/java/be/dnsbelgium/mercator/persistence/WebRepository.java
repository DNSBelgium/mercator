package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.ulid.Ulid;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SqlSourceToSinkFlow")
@Component
public class WebRepository {

  private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);
  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());

  private final String webCrawlDestination;
  private final String pageVisitDestination;
  private final String featuresDestination;
  private final ObjectMapper objectMapper;

  private final static String QUERY = """
      with
          crawl_result as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
          html_feature as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
          page_visit   as (select * exclude (year, month) from read_parquet('%s/**/*.parquet', union_by_name=True)),
          features_per_visit as (
             select visit_id,
                    list(html_feature order by crawl_timestamp) as html_features
             from html_feature
             group by visit_id
          ),
          pages_per_visit as (
             select visit_id,
                    list(page_visit order by crawl_started) as page_visits
             from page_visit
             group by visit_id
          ),
          combined as (
              select
                 crawl_result.*,
                 coalesce(features_per_visit.html_features, []) as html_features,
                 coalesce(pages_per_visit.page_visits, []) as page_visits
              from crawl_result
              left join features_per_visit on crawl_result.visit_id = features_per_visit.visit_id
              left join pages_per_visit    on crawl_result.visit_id = pages_per_visit.visit_id
              %s
          )
      select row_to_json(combined)
      from combined
      %s
    """;

  @SneakyThrows
  public WebRepository(
          ObjectMapper objectMapper,
          @Value("${mercator.data.location:mercator/data/}") String dataLocation) {
    this.objectMapper = objectMapper;
    if (dataLocation == null || dataLocation.isEmpty()) {
      throw new IllegalArgumentException("dataLocation must not be null or empty");
    }
    logger.info("dataLocation = [{}]", dataLocation);

    String subPath = "web";
    webCrawlDestination = createDestination(dataLocation, subPath, "crawl_result");
    pageVisitDestination = createDestination(dataLocation, subPath, "page_visit");
    featuresDestination = createDestination(dataLocation, subPath, "html_features");

  }

  public static boolean isURL(String dataLocation) {
    try {
      return new URI(dataLocation).getScheme() != null;
    } catch (Exception e) {
      return false;
    }
  }

  public static String createDestination(String... parts) throws IOException {
    if (isURL(parts[0])) {
      return String.join("/", parts);
    } else {
      Path destPath = Path.of(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
      Files.createDirectories(destPath);
      return destPath.toAbsolutePath().toString();
    }
  }

  public List<String> searchVisitIds(String domainName) {
    // TODO: add maxResults parameter
    logger.info("Searching visitIds for domainName={}", domainName);
    return List.of();
  }

  @SneakyThrows
  public Optional<WebCrawlResult> findLatestResult(String domainName) {
    logger.info("Finding latest crawl result for domainName={}", domainName);
    String query = QUERY.formatted(
            webCrawlDestination,
            featuresDestination,
            pageVisitDestination,
            "where crawl_result.domain_name = ?",
            " order by crawl_started desc limit 1"
    );
    Optional<String> json = jdbcClient.sql(query)
            .param(domainName)
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      try {
        WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);
        logger.debug("Found: \n{}", webCrawlResult);
        return Optional.of(webCrawlResult);
      } catch (JsonMappingException e) {
        logger.error("JsonMappingException {} for \n {}", e.getMessage(), json);
        throw e;
      }
    }
    return Optional.empty();
  }

  public Optional<WebCrawlResult> findByVisitId(String visitId) {
    // TODO
    logger.info("Finding latest crawl result for visitId={}", visitId);
    return Optional.empty();
  }


  /**
   * Converts the given JSON file to parquet format.
   */
  @SneakyThrows
  public void toParquet(Path jsonFile) {
    // generate a unique tableName since to avoid collisions with other threads
    String tableName = "webCrawlResult_" + Ulid.fast();
    String createTable = """
      create table %s
      as
      select *,
             to_timestamp(crawl_started) as ts,
             extract('year' from ts) as year,
             extract('month' from ts) as month,
      from read_json('%s')
      """.formatted(tableName, jsonFile);
    logger.info("createTable = \n{}", createTable);
    jdbcClient.sql(createTable).update();

    String copyWebCrawlResult = """
            copy (
                select * exclude (ts, html_features, page_visits)
                from %s
              )
            to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'web_{uuid}' )
            """.formatted(tableName, webCrawlDestination);
    logger.debug("copyWebCrawlResult: \n{}", copyWebCrawlResult);
    jdbcClient.sql(copyWebCrawlResult).update();

    String copyPageVisits = """
       copy (
         select
            visit_id::VARCHAR                                  as visit_id,
            domain_name::VARCHAR                               as domain_name,
            crawl_started::DOUBLE                              as crawl_started,
            crawl_finished::DOUBLE                             as crawl_finished,
            status_code::INTEGER                               as status_code,
            url::VARCHAR                                       as url,
            link_text::VARCHAR                                 as link_text,
            path::VARCHAR                                      as path,
            html::VARCHAR                                      as html,
            body_text::VARCHAR                                 as body_text,
            vat_values::VARCHAR[]                              as vat_values,
            year,
            month
         from (
                select unnest(page_visits, recursive:=True), year, month
                from %s
             )
       )
       to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'page_visit_{uuid}' )
       """
            .formatted(tableName, pageVisitDestination);
    logger.info("copyPageVisits = \n{}", copyPageVisits);
    jdbcClient.sql(copyPageVisits).update();

    String copyHtmlFeatures = """
       copy (
          select
              visit_id::VARCHAR                                  as visit_id,
              crawl_timestamp::DOUBLE                            as crawl_timestamp,
              domain_name::VARCHAR                               as domain_name,
              url::VARCHAR                                       as url,
              nb_imgs::INTEGER                                   as nb_imgs,
              nb_links_int::INTEGER                              as nb_links_int,
              nb_links_ext::INTEGER                              as nb_links_ext,
              nb_links_tel::INTEGER                              as nb_links_tel,
              nb_links_email::INTEGER                            as nb_links_email,
              nb_input_txt::INTEGER                              as nb_input_txt,
              nb_button::INTEGER                                 as nb_button,
              nb_meta_desc::INTEGER                              as nb_meta_desc,
              nb_meta_keyw::INTEGER                              as nb_meta_keyw,
              nb_numerical_strings::INTEGER                      as nb_numerical_strings,
              nb_tags::INTEGER                                   as nb_tags,
              nb_words::INTEGER                                  as nb_words,
              nb_letters::INTEGER                                as nb_letters,
              html_length::INTEGER                               as html_length,
              nb_facebook_shallow_links::INTEGER                 as nb_facebook_shallow_links,
              nb_facebook_deep_links::INTEGER                    as nb_facebook_deep_links,
              nb_linkedin_deep_links::INTEGER                    as nb_linkedin_deep_links,
              nb_linkedin_shallow_links::INTEGER                 as nb_linkedin_shallow_links,
              nb_twitter_deep_links::INTEGER                     as nb_twitter_deep_links,
              nb_twitter_shallow_links::INTEGER                  as nb_twitter_shallow_links,
              nb_youtube_deep_links::INTEGER                     as nb_youtube_deep_links,
              nb_youtube_shallow_links::INTEGER                  as nb_youtube_shallow_links,
              nb_vimeo_deep_links::INTEGER                       as nb_vimeo_deep_links,
              nb_vimeo_shallow_links::INTEGER                    as nb_vimeo_shallow_links,
              nb_currency_names::INTEGER                         as nb_currency_names,
              nb_distinct_currencies::INTEGER                    as nb_distinct_currencies,
              distance_title_final_dn::INTEGER                   as distance_title_final_dn,
              distance_title_initial_dn::INTEGER                 as distance_title_initial_dn,
              longest_subsequence_title_final_dn::INTEGER        as longest_subsequence_title_final_dn,
              longest_subsequence_title_initial_dn::INTEGER      as longest_subsequence_title_initial_dn,
              fraction_words_title_final_dn::DOUBLE              as fraction_words_title_final_dn,
              fraction_words_title_initial_dn::DOUBLE            as fraction_words_title_initial_dn,
              nb_distinct_words_in_title::INTEGER                as nb_distinct_words_in_title,
              body_text_language::VARCHAR                        as body_text_language,
              body_text_language_2::VARCHAR                      as body_text_language_2,
              nb_distinct_hosts_in_urls::INTEGER                 as nb_distinct_hosts_in_urls,
              external_hosts::VARCHAR[]                          as external_hosts,
              facebook_links::VARCHAR[]                          as facebook_links,
              twitter_links::VARCHAR[]                           as twitter_links,
              linkedin_links::VARCHAR[]                          as linkedin_links,
              youtube_links::VARCHAR[]                           as youtube_links,
              vimeo_links::VARCHAR[]                             as vimeo_links,
              title::VARCHAR                                     as title,
              htmlstruct::VARCHAR                                as htmlstruct,
              body_text::VARCHAR                                 as body_text,
              meta_text::VARCHAR                                 as meta_text,
              body_text_truncated::BOOLEAN                       as body_text_truncated,
              meta_text_truncated::BOOLEAN                       as meta_text_truncated,
              title_truncated::BOOLEAN                           as title_truncated,
              year,
              month
          from
          (
             select unnest(html_features, recursive:=True), year, month
             from %s
          )
       )
       to '%s' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'html_features_{uuid}' )
       """
            .formatted(tableName, featuresDestination);
    logger.info("copyHtmlFeatures = \n{}", copyHtmlFeatures);
    jdbcClient.sql(copyHtmlFeatures).update();
  }

  @SneakyThrows
  public List<WebCrawlResult> findByDomainName(String domainName) {
    // TODO: add limit parameter and order by crawl_timestamp desc
    String query = QUERY.formatted(
            webCrawlDestination, featuresDestination, pageVisitDestination, "where crawl_result.domain_name = ?", "");
    logger.info("query = {}", query);
    List<String> jsonList = jdbcClient.sql(query)
            .param(domainName)
            .query(String.class)
            .list();
    List<WebCrawlResult> found = new ArrayList<>();
    for (String json : jsonList) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json, WebCrawlResult.class);
      found.add(webCrawlResult);
    }
    return found;
  }



}
