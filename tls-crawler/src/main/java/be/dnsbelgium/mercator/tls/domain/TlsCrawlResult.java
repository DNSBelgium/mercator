package be.dnsbelgium.mercator.tls.domain;

import java.util.HashMap;
import java.util.Map;

public class TlsCrawlResult {

  private final Map<TlsProtocolVersion, ProtocolScanResult> scanResultMap = new HashMap<>();

  private boolean connectOK;

  private TlsProtocolVersion lowestVersionSupported;
  private TlsProtocolVersion highestVersionSupported;

  public TlsCrawlResult() {
  }

  public TlsCrawlResult(boolean connectOK) {
    this.connectOK = connectOK;
  }

  public void set(TlsProtocolVersion protocolVersion, ProtocolScanResult scanResult) {
    scanResultMap.put(protocolVersion, scanResult);
  }

}
