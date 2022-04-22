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

}
