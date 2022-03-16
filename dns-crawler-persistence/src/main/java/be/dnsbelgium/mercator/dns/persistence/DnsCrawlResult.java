package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.Records;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "dns_crawl_result")
public class DnsCrawlResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id") private Long id;
  @Column(name = "visit_id") private UUID visitId;
  @Column(name = "domain_name") private String domainName;
  @Column(name = "ok") private boolean ok;
  @Column(name = "problem") private String problem;
  @Type(type = "jsonb")
  @Column(name = "all_records", columnDefinition = "jsonb")
  private Map<String, Records> allRecords;
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "dns_crawl_result_geo_ips",
                   joinColumns=@JoinColumn(name="dns_crawl_result_id"))
  private Set<GeoIp> geoIps;
  @Column(name = "crawl_timestamp") private ZonedDateTime crawlTimestamp;

  public DnsCrawlResult(Long id, UUID visitId, String domainName, boolean ok, String problem, Map<String, Records> allRecords) {
    this.id = id;
    this.visitId = visitId;
    this.domainName = domainName;
    this.ok = ok;
    this.problem = problem;
    this.allRecords = allRecords;
    this.geoIps = new HashSet<>();
    this.crawlTimestamp = ZonedDateTime.now();
  }

  public static DnsCrawlResult of(VisitRequest request, DnsResolution resolution) {
    DnsCrawlResult dnsCrawlResult;
    if (resolution.isOk()) {
      dnsCrawlResult = new DnsCrawlResult(null, request.getVisitId(), request.getDomainName(), true, null, resolution.getRecords());
    } else {
      dnsCrawlResult = new DnsCrawlResult(null, request.getVisitId(), request.getDomainName(), false, resolution.getHumanReadableProblem(), Collections.emptyMap());
    }
    return dnsCrawlResult;
  }

  public void addGeoIp(List<GeoIp> results) {
    this.geoIps.addAll(results);
  }

}
