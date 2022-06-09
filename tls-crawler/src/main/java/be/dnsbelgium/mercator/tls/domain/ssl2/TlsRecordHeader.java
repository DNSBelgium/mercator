package be.dnsbelgium.mercator.tls.domain.ssl2;

import lombok.Data;

@Data
public class TlsRecordHeader {

  // the header will always be 2 or 3 bytes long
  public int headerLength;
  boolean securityEscape;

  // the amount of padding in the record
  public short padding;
  public short recordLength;

  public TlsRecordHeader(int recordLength, boolean securityEscape, int padding) {
    this.headerLength = (padding > 0 || securityEscape) ? 3 : 2;
    this.securityEscape = securityEscape;
    this.padding = (short) padding;
    this.recordLength = (short) recordLength;
  }

  public TlsRecordHeader(int recordLength) {
    this.headerLength = 2;
    this.recordLength = (short) recordLength;
  }

}
