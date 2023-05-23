package be.dnsbelgium.mercator.vat.crawler.persistence;

import be.dnsbelgium.mercator.common.messaging.idn.ULabelConverter;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "vat_crawl_result")
public class VatCrawlResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "visit_id")
  private UUID visitId;
  @Column(name = "domain_name")
  @Convert(converter = ULabelConverter.class)
  private String domainName;

  @Column(name = "start_url")
  private String startUrl;
  @Column(name = "matching_url")
  private String matchingUrl;

  @Column(name = "crawl_started", columnDefinition = "TIMESTAMPTZ")
  Instant crawlStarted;
  @Column(name = "crawl_finished", columnDefinition = "TIMESTAMPTZ")
  Instant crawlFinished;

  @Type(type = "jsonb")
  @Column(name = "vat_values", columnDefinition = "jsonb")
  private List<String> vatValues = new ArrayList<>();

  @Type(type = "jsonb")
  @Column(name = "visited_urls", columnDefinition = "jsonb")
  private List<String> visitedUrls = new ArrayList<>();

  public void abbreviateData() {
    domainName = StringUtils.abbreviate(domainName, 255);
    startUrl = StringUtils.abbreviate(startUrl, 255);
    matchingUrl = StringUtils.abbreviate(matchingUrl, 255);
  }
}
