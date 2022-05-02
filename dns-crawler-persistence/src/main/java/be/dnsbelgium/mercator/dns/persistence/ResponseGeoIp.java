package be.dnsbelgium.mercator.dns.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "response_geo_ips")
public class ResponseGeoIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                private Long id;
    @Column(name = "asn")               private String asn;
    @Column(name = "country")           private String country;
    @Column(name = "ip")                private String ip;
    @Column(name = "asn_organisation")  private String asnOrganisation;
    @Column(name = "ip_version")        private int ipVersion;

    @ManyToOne
    @JoinColumn(name = "response_id")   private Response response;

    public ResponseGeoIp(Pair<Integer, String> asn, String country, int ipVersion, String ip) {
        if (asn != null) {
            this.asn = String.valueOf(asn.getLeft());
            this.asnOrganisation = StringUtils.abbreviate(asn.getRight(), 128);
        }
        if (country != null) {
            this.country = StringUtils.abbreviate(country, 255);
        }
        this.ip = ip;
        this.ipVersion = ipVersion;
    }

}
