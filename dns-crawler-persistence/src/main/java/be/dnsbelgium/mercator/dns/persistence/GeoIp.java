package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;

@Embeddable
@NoArgsConstructor
@Data
public class GeoIp {

  @Enumerated(EnumType.STRING)
  @Column(name = "record_type") private RecordType recordType;
  @Column(name = "ip") private String ip;
  @Column(name = "country") private String country;
  @Column(name = "asn") private String asn;
  @Column(name = "asn_organisation") private String asnOrganisation;

  public GeoIp(RecordType recordType, String ip, String country, Pair<Integer, String> asn) {
    this.recordType = recordType;
    this.ip = ip;
    if (country != null) {
      this.country = StringUtils.abbreviate(country, 255);
    }
    if (asn != null) {
      this.asn = String.valueOf(asn.getLeft());
      this.asnOrganisation = StringUtils.abbreviate(asn.getRight(), 128);
    }
  }

}
