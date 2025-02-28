package be.dnsbelgium.mercator.tls.domain.certificates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.crypto.interfaces.DHPublicKey;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.*;
import java.security.spec.ECParameterSpec;
import java.time.Instant;
import java.util.*;

import static javax.security.auth.x500.X500Principal.RFC2253;
import static org.slf4j.LoggerFactory.getLogger;

@Data
@Builder
@AllArgsConstructor
public class Certificate {

  // Have only seen version 3 (99%) and version 1
  private final int version;

  // The serial number is an integer assigned by the certification authority to each certificate.
  // It must be unique for each certificate issued by a given CA
  private String serialNumberHex;

  // in DB, we have these values:
  // RSAPublicKey,  EllipticCurvePublicKey, DSAPublicKey, Ed25519PublicKey
  // I suggest to strip off the "PublicKey" suffix
  private final String publicKeySchema;

  private final int publicKeyLength;

  private final Instant notBefore;
  private final Instant notAfter;

  private final String issuer;
  private final String subject;

  // common values are sha256, sha384, sha1, md5, sha512
  private final String signatureHashAlgorithm;

  private Certificate signedBy;

  private final String sha256Fingerprint;

  private final List<String> subjectAlternativeNames;

  private static final Logger logger = getLogger(Certificate.class);

  private final static Map<String, String> OID_MAP = new HashMap<>();
  static {
    // there are over 2.000 OIDs but for now we only care about these:
    OID_MAP.put("2.5.4.5", "SerialNumber");
    OID_MAP.put("1.3.6.1.4.1.311.60.2.1.1", "JurisdictionOfIncorporationL");
    OID_MAP.put("1.3.6.1.4.1.311.60.2.1.2", "JurisdictionOfIncorporationSP");
    OID_MAP.put("1.3.6.1.4.1.311.60.2.1.3", "JurisdictionOfIncorporationC");
    OID_MAP.put("2.5.4.15", "BusinessCategory");
    OID_MAP.put("2.5.4.16", "PostalAddress");
  }

  public static Certificate from(X509Certificate x509Certificate) throws CertificateParsingException {
    PublicKey pubKey = x509Certificate.getPublicKey();
    return be.dnsbelgium.mercator.tls.domain.certificates.Certificate.builder()
        .issuer(x509Certificate.getIssuerX500Principal().getName())
        .subject(x509Certificate.getSubjectX500Principal().getName(RFC2253, OID_MAP))
        .version(x509Certificate.getVersion())
        .notBefore(x509Certificate.getNotBefore().toInstant())
        .notAfter(x509Certificate.getNotAfter().toInstant())
        .sha256Fingerprint(sha256_fingerprint(x509Certificate))
        .subjectAlternativeNames(getSubjectAlternativeNames(x509Certificate))
        .publicKeySchema(pubKey.getAlgorithm())
        .publicKeyLength(getKeyLength(pubKey))
        .signatureHashAlgorithm(x509Certificate.getSigAlgName())
        .serialNumberHex(convertBigIntegerToHexString(x509Certificate.getSerialNumber()))
        .build();
  }

  public String prettyString() {
    return new StringJoiner(",\n ", Certificate.class.getSimpleName() + "[\n", "]")
        .add("sha256Fingerprint=" + sha256Fingerprint)
        .add("version=" + version)
        .add("serialNumberHex=" + serialNumberHex)
        .add("publicKeySchema='" + publicKeySchema + "'")
        .add("publicKeyLength=" + publicKeyLength)
        .add("notBefore=" + notBefore)
        .add("notAfter=" + notAfter)
        .add("issuer='" + issuer + "'")
        .add("subject='" + subject + "'")
        .add("signatureHashAlgorithm='" + signatureHashAlgorithm + "'")
        .add("signedBy=" + signedBy)
        .toString();
  }

  public static int getKeyLength(PublicKey pubKey) {
    if (pubKey instanceof RSAPublicKey rsaPublicKey) {
      return rsaPublicKey.getModulus().bitLength();
    }
    if (pubKey instanceof final DSAPublicKey dsaPublicKey) {
      return dsaPublicKey.getY().bitLength();
    }
    if (pubKey instanceof ECPublicKey ecPublicKey) {
      ECParameterSpec params = ecPublicKey.getParams();
      if (params != null) {
        return params.getOrder().bitLength();
      }
    }
    if (pubKey instanceof DHPublicKey dhPublicKey) {
      return dhPublicKey.getY().bitLength();
    }
    if (pubKey instanceof XECPublicKey xecPublicKey) {
      return xecPublicKey.getU().bitLength();
    }
    logger.info("Unknown public key type: alg={} class={}", pubKey.getAlgorithm(), pubKey.getClass());
    return 0;
  }

  public static String sha256_fingerprint(X509Certificate x509Certificate) {
    try {
      byte[] encodedCertificate = x509Certificate.getEncoded();
      return DigestUtils.sha256Hex(encodedCertificate);
    } catch (CertificateEncodingException e) {
      logger.error("Encoding exception in certificate {}", x509Certificate);
      return "Encoding exception: " + e.getMessage();
    }
  }

  public static List<String> getSubjectAlternativeNames(X509Certificate x509Certificate) throws CertificateParsingException {
    List<String> subjectAlternativeNames = new ArrayList<>();
    try {
      Collection<List<?>> altNames = x509Certificate.getSubjectAlternativeNames();
      if (altNames != null) {
        for (List<?> altName : altNames) {
          if (altName.size() < 2)
            continue;
          int type = (int) altName.get(0);
          Object data = altName.get(1);
          switch(type) {
            case GeneralName.dNSName:
            case GeneralName.iPAddress:
              if (data instanceof String) {
                subjectAlternativeNames.add(data.toString());
              }
              break;
            default:
              logger.debug("Subject Alt Name of type {} with value {}", type, data);
          }
        }
      }
    } catch (CertificateParsingException e) {
      logger.error("Could not parse SAN's from certificate {} because of {}", x509Certificate, e.getMessage());
      throw e;
    }
    return subjectAlternativeNames;
  }

  public static String convertBigIntegerToHexString(BigInteger bigInteger){
    if (bigInteger == null || bigInteger.compareTo(BigInteger.valueOf(0)) < 0){
      return null;
    }
    String hexString = bigInteger.toString(16);
    if (hexString.length() % 2 == 1){
      hexString = "0" + hexString;
    }
    hexString = hexString.replaceAll("(.{2})", "$1:");
    if (hexString.endsWith(":")){
      hexString = hexString.substring(0, hexString.length() - 1);
    }
    return hexString;
  }

}
