package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import be.dnsbelgium.mercator.ssl.crawler.persistence.CheckAgainstTrustStoreCompositeId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@IdClass(CheckAgainstTrustStoreCompositeId.class)
@Table(name = "check_against_trust_store")
public class CheckAgainstTrustStore {

  @Id
  @Column(name="certificate_deployment_id")
  private Long certificateDeploymentId;

  @Id
  @Column(name="trust_store_id")
  private Long trustStoreId;

  @Column(name = "valid")
  private Boolean valid;

}
