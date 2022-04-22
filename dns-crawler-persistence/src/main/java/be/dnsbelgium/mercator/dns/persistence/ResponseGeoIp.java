package be.dnsbelgium.mercator.dns.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "response")
public class ResponseGeoIp {

    @Column(name = "asn")               private String asn;
    @Column(name = "country")           private String country;
    @Column(name = "ip")                private String ip;
    @Column(name = "asn_organisation")  private String asnOrganisation;
    @Column(name = "ip_version")        private int ipVersion;
    @ManyToOne
    @JoinColumn (name = "response_id")   private Response response;

}
