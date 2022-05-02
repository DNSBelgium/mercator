package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;
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
    @Column(name = "crawl_timestamp")   private ZonedDateTime crawlTimestamp;
    @Column(name = "ok")                private boolean ok;
    @Column(name = "problem")           private String problem;

}