package be.dnsbelgium.mercator.feature.extraction.persistence;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
public class HtmlFeatures {

  public String visitId;
  public Instant crawlTimestamp;

  public String domainName;

  public String url;

  // using public fields and the same names as the python code did
  public int nb_imgs;
  public int nb_links_int;
  public int nb_links_ext;
  public int nb_links_tel;
  public int nb_links_email;
  public int nb_input_txt;
  public int nb_button;
  public int nb_meta_desc;
  public int nb_meta_keyw;
  public int nb_numerical_strings;
  public int nb_tags;
  public int nb_words;
  public long nb_letters;
  public long html_length;


  public Integer nb_facebook_shallow_links;
  public Integer nb_facebook_deep_links;
  public Integer nb_linkedin_deep_links;
  public Integer nb_linkedin_shallow_links;
  public Integer nb_twitter_deep_links;
  public Integer nb_twitter_shallow_links;
  public Integer nb_youtube_deep_links;
  public Integer nb_youtube_shallow_links;
  public Integer nb_vimeo_deep_links;
  public Integer nb_vimeo_shallow_links;

  public Integer nb_currency_names;
  public Integer nb_distinct_currencies;
  public Integer distance_title_final_dn;
  public Integer distance_title_initial_dn;
  public Integer longest_subsequence_title_final_dn;
  public Integer longest_subsequence_title_initial_dn;
  public Float fraction_words_title_final_dn;
  public Float fraction_words_title_initial_dn;
  public Integer nb_distinct_words_in_title;

  public String body_text_language;
  public String body_text_language_2;

  @Builder.Default
  public Integer nb_distinct_hosts_in_urls = 0;

  @Builder.Default
  public List<String> external_hosts = Collections.emptyList();

  @Builder.Default
  public List<String> facebook_links = Collections.emptyList();

  @Builder.Default
  public List<String> twitter_links = Collections.emptyList();

  @Builder.Default
  public List<String> linkedin_links = Collections.emptyList();

  @Builder.Default
  public List<String> youtube_links = Collections.emptyList();

  @Builder.Default
  public List<String> vimeo_links = Collections.emptyList();

  public String title;
  public String htmlstruct;
  public String body_text;
  public String meta_text;

  public boolean body_text_truncated;
  public boolean meta_text_truncated;
  public boolean title_truncated;

  @Override
  public String toString() {
    return new StringJoiner(", \n", HtmlFeatures.class.getSimpleName() + "[", "]")
        .add("domainName=" + domainName)
        .add("external_hosts=" + external_hosts)
        .add("crawlTimestamp=" + crawlTimestamp)
        .add("nb_imgs=" + nb_imgs)
        .add("nb_links_int=" + nb_links_int)
        .add("nb_links_ext=" + nb_links_ext)
        .add("nb_links_tel=" + nb_links_tel)
        .add("nb_links_email=" + nb_links_email)
        .add("nb_input_txt=" + nb_input_txt)
        .add("nb_button=" + nb_button)
        .add("nb_meta_desc=" + nb_meta_desc)
        .add("nb_meta_keyw=" + nb_meta_keyw)
        .add("nb_numerical_strings=" + nb_numerical_strings)
        .add("nb_distinct_hosts_in_urls=" + nb_distinct_hosts_in_urls)
        .add("nb_tags=" + nb_tags)
        .add("nb_words=" + nb_words)
        .add("nb_letters=" + nb_letters)
        .add("title='" + title + "'")
        .add("htmlstruct='" + StringUtils.abbreviate(htmlstruct, 60) + "'")
        .add("body_text='" + StringUtils.abbreviate(body_text, 60) + "'")
        .add("meta_text='" + StringUtils.abbreviate(meta_text, 60) + "'")
        .add("nb_facebook_shallow_links=" + nb_facebook_shallow_links)
        .add("nb_facebook_deep_links=" + nb_facebook_deep_links)
        .add("facebook_links=" + facebook_links)
        .add("nb_linkedin_deep_links=" + nb_linkedin_deep_links)
        .add("nb_linkedin_shallow_links=" + nb_linkedin_shallow_links)
        .add("linkedin_links=" + linkedin_links)
        .add("nb_twitter_deep_links=" + nb_twitter_deep_links)
        .add("nb_twitter_shallow_links=" + nb_twitter_shallow_links)
        .add("twitter_links=" + twitter_links)
        .add("nb_youtube_deep_links=" + nb_youtube_deep_links)
        .add("nb_youtube_shallow_links=" + nb_youtube_shallow_links)
        .add("youtube_links=" + youtube_links)
        .add("nb_vimeo_deep_links=" + nb_vimeo_deep_links)
        .add("nb_vimeo_shallow_links=" + nb_vimeo_shallow_links)
        .add("vimeo_links=" + vimeo_links)
        .add("nb_currency_names=" + nb_currency_names)
        .add("nb_distinct_currencies=" + nb_distinct_currencies)
        .add("distance_url_title=" + distance_title_final_dn)
        .add("distance_dn_title=" + distance_title_initial_dn)
        .add("substring_url_title=" + longest_subsequence_title_final_dn)
        .add("substring_dn_title=" + longest_subsequence_title_initial_dn)
        .add("fraction_words_in_dn=" + fraction_words_title_initial_dn)
        .add("fraction_words_in_url=" + fraction_words_title_final_dn)
        .add("nb_distinct_words_in_title=" + nb_distinct_words_in_title)
        .add("body_text_languages=" + body_text_language)
        .add("body_text_languages_2=" + body_text_language_2)
        .toString();
  }

}
