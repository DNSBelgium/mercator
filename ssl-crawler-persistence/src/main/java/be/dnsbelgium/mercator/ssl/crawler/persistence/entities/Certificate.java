package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@ToString
@Embeddable
@Table(name = "certificate")
public class Certificate {

  @Id
  @Column(name = "sha256_fingerprint")
  private String sha256fingerprint;

  @Column(name = "version")
  private String version;

  @Column(name = "serial_number")
  private String serialNumber;

  @Column(name = "public_key_schema")
  private String publicKeySchema;

  @Column(name = "public_key_length")
  private Integer publicKeyLength;

  @Column(name = "not_before", columnDefinition = "TIMESTAMPTZ")
  private Instant notBefore;

  @Column(name = "not_after", columnDefinition = "TIMESTAMPTZ")
  private Instant notAfter;

  @Column(name = "issuer")
  private String issuer;

  @Column(name = "subject")
  private String subject;

  @Column(name = "signature_hash_algorithm")
  private String signatureHashAlgorithm;

  @Column(name = "signed_by_sha256")
  private String signedBySha256;

}