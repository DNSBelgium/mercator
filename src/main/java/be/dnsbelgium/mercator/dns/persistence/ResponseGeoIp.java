package be.dnsbelgium.mercator.dns.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Data
@NoArgsConstructor
public class ResponseGeoIp {

    private Long id;
    private String asn;
    private String country;
    private String ip;
    private String asnOrganisation;
    private int ipVersion;

    public ResponseGeoIp(Pair<Long, String> asn, String country, int ipVersion, String ip) {
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
