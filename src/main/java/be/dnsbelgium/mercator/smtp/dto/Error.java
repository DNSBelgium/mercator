package be.dnsbelgium.mercator.smtp.dto;

public enum Error {
  TIME_OUT,
  CONNECTION_ERROR,
  TLS_ERROR,
  HOST_UNREACHABLE,
  SKIPPED,
  CHANNEL_CLOSED,
  UNEXPECTED_REPLY_CODE,
  OTHER
}
