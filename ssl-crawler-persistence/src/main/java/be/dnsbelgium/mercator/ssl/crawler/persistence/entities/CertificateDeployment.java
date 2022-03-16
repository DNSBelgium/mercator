package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@Embeddable
@Table(name = "certificate_deployment")
public class CertificateDeployment {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "ssl_crawl_result_id")
  private Long sslCrawlResultId;

  @Column(name = "leaf_certificate_sha256")
  private String leafCertificateSha256;

  @Column(name = "length_received_certificate_chain")
  private Integer lengthReceivedCertificateChain;

  @Column(name = "leaf_certificate_subject_matches_hostname")
  private Boolean leafCertificateSubjectMatchesHostname;

  @Column(name = "leaf_certificate_has_must_staple_extension")
  private Boolean leafCertificateHasMustStapleExtension;

  @Column(name = "leaf_certificate_is_ev")
  private Boolean leafCertificateIsEv;

  @Column(name = "received_chain_contains_anchor_certificate")
  private Boolean receivedChainContainsAnchorCertificate;

  @Column(name = "received_chain_has_valid_order")
  private Boolean receivedChainHasValidOrder;

  @Column(name = "verified_chain_has_sha1_signature")
  private Boolean verifiedChainHasSha1Signature;

  @Column(name = "verified_chain_has_legacy_symantec_anchor")
  private Boolean verifiedChainHasLegacySymantecAnchor;

  @Column(name = "ocsp_response_is_trusted")
  private Boolean ocspResponseIsTrusted;

  @ElementCollection(fetch = FetchType.EAGER)
  @JoinColumns({ @JoinColumn(name = "certificate_deployment_id", referencedColumnName = "id")})
  private Set<CheckAgainstTrustStore> checksAgainstTrustStores;


}
