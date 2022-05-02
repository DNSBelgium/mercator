package be.dnsbelgium.mercator.dns.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")                private Long id;
    @Column(name = "record_data")       private String recordData;
    @Column(name = "ttl")               private int ttl;
    @ManyToOne
    @JoinColumn (name = "request_id")   private Request request;

}
