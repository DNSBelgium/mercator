package be.dnsbelgium.mercator.ssl.crawler.persistence.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@ToString
@Table(name = "cipher_suite")
public class CipherSuite {

  @Id
  @Column(name = "iana_name")
  private String ianaName;

  @Column(name = "openssl_name")
  private String opensslName;

  @Column(name = "key_exchange_algorithm")
  private String keyExchangeAlgorithm;

  @Column(name = "authentication_algorithm")
  private String authenticationAlgorithm;

  @Column(name = "encryption_algorithm")
  private String encryptionAlgorithm;

  @Column(name = "hash_algorithm")
  private String hashAlgorithm;

  @Column(name = "security")
  private String security;

}
