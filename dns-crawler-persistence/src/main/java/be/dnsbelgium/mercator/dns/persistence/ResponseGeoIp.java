package be.dnsbelgium.mercator.dns.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;

@Embeddable
@NoArgsConstructor
@Data
public class ResponseGeoIp {

    @Column(name = "asn")               private String asn;
    @Column(name = "country")           private String country;
    @Column(name = "ip")                private String ip;
    @Column(name = "asn_organisation")  private String asnOrganisation;
    @Column(name = "ip_version")        private int ipVersion;
    @ManyToOne
    @JoinColumn (name = "response_id")  private Response response;

    // TODO: AvR check necessity.
    public ResponseGeoIp(int ipVersion, String ip, String country, Pair<Integer, String> asn) {
        this.ipVersion = ipVersion;
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
