package be.dnsbelgium.mercator.dns.persistence;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "request")
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

    private Request(Builder builder) {
        setId(builder.id);
        setVisitId(builder.visitId);
        setDomainName(builder.domainName);
        setPrefix(builder.prefix);
        setRecordType(builder.recordType);
        setRcode(builder.rcode);
        setCrawlTimestamp(builder.crawlTimestamp);
        setOk(builder.ok);
        setProblem(builder.problem);
    }

    public static final class Builder {
        private Long id;
        private UUID visitId;
        private String domainName;
        private String prefix;
        private RecordType recordType;
        private int rcode;
        private ZonedDateTime crawlTimestamp;
        private boolean ok;
        private String problem;

        public Builder() {
        }

        public Builder id(Long val) {
            id = val;
            return this;
        }

        public Builder visitId(UUID val) {
            visitId = val;
            return this;
        }

        public Builder domainName(String val) {
            domainName = val;
            return this;
        }

        public Builder prefix(String val) {
            prefix = val;
            return this;
        }

        public Builder recordType(RecordType val) {
            recordType = val;
            return this;
        }

        public Builder rcode(int val) {
            rcode = val;
            return this;
        }

        public Builder crawlTimestamp(ZonedDateTime val) {
            crawlTimestamp = val;
            return this;
        }

        public Builder ok(boolean val) {
            ok = val;
            return this;
        }

        public Builder problem(String val) {
            problem = val;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }
}