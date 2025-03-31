package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WebRepository extends BaseRepository<WebCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);

  private final String webCrawlDestination;
  private final String pageVisitDestination;
  private final String featuresDestination;



  @Override
  public String getAllItemsQuery() {
    return StringSubstitutor.replace("""
      with
        crawl_result as (select * exclude (year, month) from read_parquet('${webCrawlDestination}/**/*.parquet', union_by_name=True)),
        html_feature as (select * exclude (year, month) from read_parquet('${featuresDestination}/**/*.parquet', union_by_name=True)),
        page_visit   as (select * exclude (year, month) from read_parquet('${pageVisitDestination}/**/*.parquet', union_by_name=True)),
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
      )
      from combined
    """, Map.of(
      "webCrawlDestination", this.webCrawlDestination,
      "featuresDestination", this.featuresDestination,
      "pageVisitDestination", this.pageVisitDestination
    ));
  }

  @SneakyThrows
  public WebRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
    super(objectMapper, baseLocation, WebCrawlResult.class);
    String subPath = "web";
    webCrawlDestination = createDestination(baseLocation, subPath, "crawl_result");
    pageVisitDestination = createDestination(baseLocation, subPath, "page_visit");
    featuresDestination = createDestination(baseLocation, subPath, "html_features");
  }

  @Override
  public String timestampField() {
    return "crawl_started";
  }

  @Override
  public void storeResults(String jsonResultsLocation) {
    String allResultsQuery = StringSubstitutor.replace("""
      select *,
             year(to_timestamp(${timestampField})) as year,
             month(to_timestamp(${timestampField})) as month
      from read_json('${jsonFile}', field_appearance_threshold=1)
      """, Map.of(
        "timestampField", this.timestampField(),
        "jsonFile", jsonResultsLocation
    ));
    logger.info("allResultsQuery = \n{}", allResultsQuery);

    String copyWebCrawlResult = StringSubstitutor.replace("""
            copy (
                with all_results as (
                  ${allResultsQuery}
                )
                select * exclude (html_features, page_visits)
                from all_results
              )
            to '${webCrawlDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'web_{uuid}' )
            """, Map.of(
              "allResultsQuery", allResultsQuery,
              "webCrawlDestination", this.webCrawlDestination
            )
    );
    logger.debug("copyWebCrawlResult: \n{}", copyWebCrawlResult);
    getJdbcClient().sql(copyWebCrawlResult).update();

    String copyPageVisits = StringSubstitutor.replace("""
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
            response_body::VARCHAR                             as response_body,
            vat_values::VARCHAR[]                              as vat_values,
            year,
            month
         from (
                select unnest(page_visits, recursive:=True), year, month
                from (${allResultsQuery})
             )
       )
       to '${pageVisitDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'page_visit_{uuid}' )
       """, Map.of(
            "allResultsQuery", allResultsQuery,
            "pageVisitDestination", this.pageVisitDestination
        )
    );
    logger.info("copyPageVisits = \n{}", copyPageVisits);
    getJdbcClient().sql(copyPageVisits).update();

    String copyHtmlFeatures = StringSubstitutor.replace("""
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
             from (${allResultsQuery})
          )
       )
       to '${featuresDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'html_features_{uuid}' )
       """, Map.of(
            "allResultsQuery", allResultsQuery,
            "featuresDestination", this.featuresDestination
        )
    );
    logger.info("copyHtmlFeatures = \n{}", copyHtmlFeatures);
    getJdbcClient().sql(copyHtmlFeatures).update();
  }

}
