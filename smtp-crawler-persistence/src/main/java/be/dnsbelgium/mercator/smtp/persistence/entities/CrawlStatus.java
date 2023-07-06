package be.dnsbelgium.mercator.smtp.persistence.entities;

public enum CrawlStatus {
  OK,
  NETWORK_ERROR,
  INVALID_HOSTNAME,
  INTERNAL_ERROR,
  NO_REACHABLE_SMTP_SERVERS
}
