package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Getter
@ToString
@Table(name = "tls_scan_result")
public class TlsScanResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private UUID visitId;

  private String prefix;

  private String domainName;

  @Column(name = "crawl_timestamp")
  private ZonedDateTime crawlTimestamp;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "scan_result")
  private ScanResult scanResult;

}
