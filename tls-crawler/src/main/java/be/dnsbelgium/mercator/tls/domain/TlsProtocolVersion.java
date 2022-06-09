package be.dnsbelgium.mercator.tls.domain;

import java.util.HashMap;
import java.util.Map;

public enum TlsProtocolVersion {

  SSL_2   (new byte[] { (byte) 0x00, (byte) 0x02 }, "SSLv2"),
  SSL_3   (new byte[] { (byte) 0x03, (byte) 0x00 }, "SSLv3"),
  TLS_1_0  (new byte[] { (byte) 0x03, (byte) 0x01 }, "TLSv1"),
  TLS_1_1  (new byte[] { (byte) 0x03, (byte) 0x02 }, "TLSv1.1"),
  TLS_1_2  (new byte[] { (byte) 0x03, (byte) 0x03 }, "TLSv1.2"),
  TLS_1_3  (new byte[] { (byte) 0x03, (byte) 0x04 }, "TLSv1.3");

  private final byte[] value;
  private final String name;

  public static final int BITS_IN_A_BYTE = 8;

  TlsProtocolVersion(byte[] value, String name) {
    this.value = value;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  private static final Map<Integer, TlsProtocolVersion> MAP;
  static {
    MAP = new HashMap<>();
    for (TlsProtocolVersion c : TlsProtocolVersion.values()) {
      MAP.put(c.valueAsInt(), c);
    }
  }

  private static int valueAsInt(byte[] value) {
    if (value.length == 2) {
      return (value[0] & 0xff) << BITS_IN_A_BYTE | (value[1] & 0xff);
    }
    throw new IllegalStateException("value should consist of two bytes but has " +  value.length + " bytes.");
  }

  int valueAsInt() {
    return valueAsInt(this.value);
  }

  static TlsProtocolVersion fromInt(int value) {
    return MAP.get(value);
  }

  public static TlsProtocolVersion from(byte[] bytes) {
    int key = valueAsInt(bytes);
    return MAP.get(key);
  }

  public byte[] getValue() {
    return value;
  }
}
