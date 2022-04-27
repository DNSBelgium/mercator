package be.dnsbelgium.mercator.api.dns;

import lombok.Data;

@Data
public class GeoIpDTO { // Might not need. WIP.

    private String asn;
    private String country;
    private String ip;
    private String asnOrganisation;
    private int ipVersion;

    // Builder
    private GeoIpDTO(Builder builder) {
        setAsn(builder.asn);
        setCountry(builder.country);
        setIp(builder.ip);
        setAsnOrganisation(builder.asnOrganisation);
        setIpVersion(builder.ipVersion);
    }


    public static final class Builder {
        private String asn;
        private String country;
        private String ip;
        private String asnOrganisation;
        private int ipVersion;

        public Builder() {
        }

        public Builder asn(String val) {
            asn = val;
            return this;
        }

        public Builder country(String val) {
            country = val;
            return this;
        }

        public Builder ip(String val) {
            ip = val;
            return this;
        }

        public Builder asnOrganisation(String val) {
            asnOrganisation = val;
            return this;
        }

        public Builder ipVersion(int val) {
            ipVersion = val;
            return this;
        }

        public GeoIpDTO build() {
            return new GeoIpDTO(this);
        }
    }
}
