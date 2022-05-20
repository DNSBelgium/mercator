package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

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

    // RESPONSES
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinColumn(name = "request_id")
    @Builder.Default                    private List<Response> responses = new ArrayList<>();
                                        private int numOfResponses;

    // SIGNATURES
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinColumn(name = "request_id")
    @Builder.Default                    private List<RecordSignature> recordSignatures = new ArrayList<>();
//                                        private int numOfSignatures;

    @Access(AccessType.PROPERTY)
    @Column(name = "num_of_responses")
    public int getNumOfResponses() {
        return this.responses.size();
    }

}