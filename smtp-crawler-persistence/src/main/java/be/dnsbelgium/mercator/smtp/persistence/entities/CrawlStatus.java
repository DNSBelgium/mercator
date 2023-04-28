package be.dnsbelgium.mercator.smtp.persistence.entities;

public enum CrawlStatus {

  // See https://dmap.sidnlabs.nl/datamodel.html
  OK,
  MALFORMED_URL,
  TIME_OUT,
  UNKNOWN_HOST,
  NETWORK_ERROR,
  CONNECTION_REFUSED,
  PROTOCOL_ERROR,
  INVALID_HOSTNAME,
  NO_IP_ADDRESS,
  INTERNAL_ERROR

}
