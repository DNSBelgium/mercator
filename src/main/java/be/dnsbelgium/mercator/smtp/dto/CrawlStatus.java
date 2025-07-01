package be.dnsbelgium.mercator.smtp.dto;

public enum CrawlStatus {
  OK,
  SKIPPED,
  NETWORK_ERROR,
  INVALID_HOSTNAME,
  INTERNAL_ERROR,
  NO_REACHABLE_SMTP_SERVERS
}
