package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Getter
@ToString
@Table(name = "certificate")
public class Certificate {

  @Id
  @Column(name = "sha256_fingerprint")
  private String sha256fingerprint;

  @Column(name = "version")
  private int version;

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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  public List<String> subjectAltNames = Collections.emptyList();

}
