package be.dnsbelgium.mercator.tls.domain.certificates;

// see java.security.cert.X509Certificate.getSubjectAlternativeNames

@SuppressWarnings("unused")
public class GeneralName {
  public static final int otherName = 0;
  public static final int rfc822Name = 1;
  public static final int dNSName = 2;
  public static final int x400Address = 3;
  public static final int directoryName = 4;
  public static final int ediPartyName = 5;
  public static final int uniformResourceIdentifier = 6;
  public static final int iPAddress = 7;
  public static final int registeredID = 8;
}
