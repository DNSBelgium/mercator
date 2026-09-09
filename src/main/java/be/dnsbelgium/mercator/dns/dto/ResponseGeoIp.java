package be.dnsbelgium.mercator.dns.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Data
@NoArgsConstructor
public class ResponseGeoIp {

    private String asn;
    private String country;
    private String ip;
    private String asnOrganisation;
    private int ipVersion;

    /**
     * Builds a {@link ResponseGeoIp} from a GeoIP lookup result. Kept as a static factory (rather
     * than a constructor) so it is not mistaken for a Jackson creator: the JSON round-trip relies on
     * the no-arg constructor plus setters, exactly like it did under the legacy Jackson 2 mapper.
     */
    public static ResponseGeoIp of(Pair<Long, String> asn, String country, int ipVersion, String ip) {
        ResponseGeoIp result = new ResponseGeoIp();
        if (asn != null) {
            result.asn = String.valueOf(asn.getLeft());
            result.asnOrganisation = StringUtils.abbreviate(asn.getRight(), 128);
        }
        if (country != null) {
            result.country = StringUtils.abbreviate(country, 255);
        }
        result.ip = ip;
        result.ipVersion = ipVersion;
        return result;
    }

}
