package be.dnsbelgium.mercator.vat.crawler.persistence;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
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
@ToString
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "page_visit")
public class PageVisit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id") private Long id;

  @Column(name = "visit_id") private UUID visitId;
  @Column(name = "domain_name") private String domainName;

  // according to https://stackoverflow.com/questions/6627289/what-is-the-most-recommended-way-to-store-time-in-postgresql-using-java
  // we should use java.time.Instant and TIMESTAMPTZ to store timestamps in Postgres

  @Column(name="crawl_started", columnDefinition="TIMESTAMPTZ")  Instant crawlStarted;
  @Column(name="crawl_finished", columnDefinition="TIMESTAMPTZ") Instant crawlFinished;

  @Column(name = "status_code") private Integer statusCode;

  @Column(name = "url") private String url;

  @Column(name = "link_text") private String linkText;

  @Column(name = "path") private String path;

  @Column(name = "body_text", columnDefinition="TEXT")
  private String bodyText;

  @Type(type = "jsonb")
  @Column(name = "vat_values", columnDefinition = "jsonb")
  private List<String> vatValues = new ArrayList<>();


  public PageVisit() {
  }

  public PageVisit(
      UUID visitId,
      String domainName,
      String url,
      String path,
      Instant crawlStarted,
      Instant crawlFinished,
      int statusCode,
      String bodyText,
      List<String> vatValues) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.url  = cleanUp(url, 500);
    this.path = cleanUp(path,500);
    this.crawlStarted = crawlStarted;
    this.crawlFinished = crawlFinished;
    this.statusCode = statusCode;
    this.bodyText = cleanUp(bodyText, 20_000);
    this.vatValues = vatValues;
  }

  private String cleanUp(String input, int maxLength) {
    if (input == null) {
      return null;
    }
    if (input.contains("\u0000")) {
      return null;
    }
    return StringUtils.abbreviate(input, maxLength);
  }

  public void setLinkText(String linkText) {
    this.linkText  = cleanUp(linkText, 500);
  }

}
