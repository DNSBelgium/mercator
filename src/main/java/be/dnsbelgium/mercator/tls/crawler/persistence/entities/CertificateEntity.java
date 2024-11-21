package be.dnsbelgium.mercator.tls.crawler.persistence.entities;

import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
@Getter
@ToString
public class CertificateEntity {

  private String sha256fingerprint;

  private int version;

  private String serialNumberHex;

  private String publicKeySchema;

  private Integer publicKeyLength;

  private Instant notBefore;

  private Instant notAfter;

  private String issuer;

  private String subject;

  private String signatureHashAlgorithm;

  private String signedBySha256;

  @Builder.Default
  public List<String> subjectAltNames = Collections.emptyList();

}
