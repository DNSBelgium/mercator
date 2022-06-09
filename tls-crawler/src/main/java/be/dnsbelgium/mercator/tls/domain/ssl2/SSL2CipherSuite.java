package be.dnsbelgium.mercator.tls.domain.ssl2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/*
  Copied from de.rub.nds.tlsattacker.core.constants.SSL2CipherSuite (https://github.com/tls-attacker/TLS-Attacker)
  Apache License 2.0
  see https://github.com/tls-attacker/TLS-Attacker/blob/master/LICENSE

  Modified to remove unused fields and the dependency on BouncyCastle. See https://www.bouncycastle.org/licence.html

 */
public enum SSL2CipherSuite implements Serializable {
  SSL_CK_RC4_128_WITH_MD5(0x010080),
  SSL_CK_RC4_128_EXPORT40_WITH_MD5(0x020080),
  SSL_CK_RC2_128_CBC_WITH_MD5(0x030080),
  SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5(0x040080),
  SSL_CK_IDEA_128_CBC_WITH_MD5(0x050080),
  SSL_CK_DES_64_CBC_WITH_MD5(0x060040),
  SSL_CK_DES_192_EDE3_CBC_WITH_MD5(0x0700C0),
  SSL_UNKNOWN_CIPHER(0x999999);

  private static final int SSL2CipherSuiteLength = 3;
  private final int value;

  private static final Map<Integer, SSL2CipherSuite> MAP;

  SSL2CipherSuite(int value) {
    this.value = value;
  }

  static {
    MAP = new HashMap<>();
    for (SSL2CipherSuite c : SSL2CipherSuite.values()) {
      MAP.put(c.value, c);
    }
  }

  public static List<SSL2CipherSuite> validCiphers() {
    return Arrays.stream(values())
        .filter(cipherSuite -> cipherSuite != SSL_UNKNOWN_CIPHER)
        .collect(Collectors.toList());
  }

  public static byte[] intToBytes(int value, int size) {
    if (size < 1) {
      throw new IllegalArgumentException("The array must be at least of size 1");
    } else {
      byte[] result = new byte[size];
      int shift = 0;
      int finalPosition = size > 4 ? size - 4 : 0;

      for(int i = size - 1; i >= finalPosition; --i) {
        result[i] = (byte)(value >>> shift);
        shift += 8;
      }

      return result;
    }
  }

  public static int bytesToInt(byte[] value) {
    int result = 0;
    int shift = 0;

    for(int i = value.length - 1; i >= 0; --i) {
      result += (value[i] & 255) << shift;
      shift += 8;
    }

    return result;
  }

  /**
   * Make a copy of a range of bytes from the passed in array. The range can extend beyond the end
   * of the input array, in which case the returned array will be padded with zeroes.
   *
   * @param original
   *            the array from which the data is to be copied.
   * @param from
   *            the start index at which the copying should take place.
   * @param to
   *            the final index of the range (exclusive).
   *
   * @return a new byte array containing the range given.
   */
  public static byte[] copyOfRange(byte[] original, int from, int to)
  {
    int newLength = getLength(from, to);
    byte[] copy = new byte[newLength];
    System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
    return copy;
  }

  private static int getLength(int from, int to)
  {
    int newLength = to - from;
    if (newLength < 0)
    {
      throw new IllegalArgumentException(" > " + to);
    }
    return newLength;
  }


  public byte[] getByteValue() {
    return intToBytes(value, SSL2CipherSuiteLength);
  }

  public static List<SSL2CipherSuite> getCipherSuites(byte[] values) {
    List<SSL2CipherSuite> cipherSuites = new LinkedList<>();
    int pointer = 0;
    while (pointer < values.length) {
      byte[] suiteBytes = copyOfRange(values, pointer, pointer + SSL2CipherSuiteLength);
      int suiteValue = bytesToInt(suiteBytes);
      cipherSuites.add(getCipherSuite(suiteValue));
      pointer += SSL2CipherSuiteLength;
    }
    return cipherSuites;
  }

  public static SSL2CipherSuite getCipherSuite(int value) {
    SSL2CipherSuite cs = MAP.get(value);
    if (cs == null) {
      return SSL_UNKNOWN_CIPHER;
    }
    return cs;
  }

}
