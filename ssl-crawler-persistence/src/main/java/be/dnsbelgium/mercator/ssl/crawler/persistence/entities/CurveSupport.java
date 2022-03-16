package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import be.dnsbelgium.mercator.ssl.crawler.persistence.CurveSupportCompositeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@IdClass(CurveSupportCompositeId.class)
@Table(name = "curve_support")
public class CurveSupport {

  @Id
  @Column(name = "ssl_crawl_result_id")
  private Long sslCrawlResultId;

  @Id
  @Column(name = "curve_id")
  private Long curveId;

  @Column(name = "supported")
  private Boolean supported;

}