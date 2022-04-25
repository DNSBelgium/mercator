package be.dnsbelgium.mercator.dns.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "response")
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                private Long id;
    @Column(name = "record_data")       private String recordData;
    @Column(name = "ttl")               private String ttl;
    @ManyToOne
    @JoinColumn (name = "request_id")   private Request request;

    private Response(Builder builder) {
        setId(builder.id);
        setRecordData(builder.recordData);
        setTtl(builder.ttl);
        setRequest(builder.request);
    }


    public static final class Builder {
        private Long id;
        private String recordData;
        private String ttl;
        private Request request;

        public Builder() {
        }

        public Builder id(Long val) {
            id = val;
            return this;
        }

        public Builder recordData(String val) {
            recordData = val;
            return this;
        }

        public Builder ttl(String val) {
            ttl = val;
            return this;
        }

        public Builder request(Request val) {
            request = val;
            return this;
        }

        public Response build() {
            return new Response(this);
        }
    }
}
