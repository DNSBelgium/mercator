package be.dnsbelgium.mercator.dns.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "record_signature")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                private Long id;
    @Column(name = "key_tag")           private Long keyTag;
    @Column(name = "algorithm")         private Long algorithm;
    @Column(name = "labels")            private Long labels;
    @Column(name = "ttl")               private Long ttl;
    @Column(name = "inception_date")    private ZonedDateTime inceptionDate;
    @Column(name = "expiration_date")   private ZonedDateTime expirationDate;
    @Column(name = "signer")            private String signer;

}
