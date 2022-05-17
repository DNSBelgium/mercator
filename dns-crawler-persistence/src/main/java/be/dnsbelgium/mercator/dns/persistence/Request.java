package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                private Long id;
    @Column(name = "visit_id")          private UUID visitId;
    @Column(name = "domain_name")       private String domainName;
    @Column(name = "prefix")            private String prefix;
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")       private RecordType recordType;
    @Column(name = "rcode")             private int rcode;
    @Builder.Default
    @Column(name = "crawl_timestamp")   private ZonedDateTime crawlTimestamp = ZonedDateTime.now();
    @Column(name = "ok")                private boolean ok;
    @Column(name = "problem")           private String problem;
    @Column(name = "num_of_responses")  private int numOfResponses;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "request_id")
    @Builder.Default                    private List<Response> responses = new ArrayList<>();

}