package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
import be.dnsbelgium.mercator.tls.domain.certificates.Trust;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2Client;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2ScanResult;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsScanner {

  private static final Logger logger = getLogger(TlsScanner.class);
  private static final int DEFAULT_PORT = 443;

  private final Map<TlsProtocolVersion, SSLSocketFactory> factoryMap = new HashMap<>();

  private final boolean ipv6Enabled;

  private final boolean verbose;

  private final Duration connectTimeOut;
  private final Duration readTimeOut;

  private final SSL2Client ssl2Client;

  private final HostnameVerifier hostnameVerifier;

  public final static int DEFAULT_CONNECT_TIME_OUT_MS = 5000;
  public final static int DEFAULT_READ_TIME_OUT_MS = 5000;

  public static TlsScanner standard() {
    return new TlsScanner(new DefaultHostnameVerifier(), false, false, DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
  }

  @SneakyThrows
  public TlsScanner(
      HostnameVerifier hostnameVerifier,
      @Value("${tls.scanner.ipv6.enabled:false}") boolean ipv6Enabled,
      @Value("${tls.scanner.verbose:false}") boolean verbose,
      @Value("${tls.scanner.connect.timeout.milliSeconds:3000}") int connectTimeOutMilliSeconds,
      @Value("${tls.scanner.read.timeout.milliSeconds:3000}") int readTimeOutMilliSeconds) {
    this.hostnameVerifier = hostnameVerifier;
    this.verbose = verbose;
    this.ipv6Enabled = ipv6Enabled;
    this.connectTimeOut = Duration.ofMillis(connectTimeOutMilliSeconds);
    this.readTimeOut    = Duration.ofMillis(readTimeOutMilliSeconds);
    logger.info("Creating TlsScanner with connectTimeOut={}, readTimeOut={}", connectTimeOut, readTimeOut);

    TrustManager trustManager = Trust.trustAnythingTrustManager();

    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      // Modern java versions do not support SSL v2 => do not try to create SSLSocketFactory
      if (version != TlsProtocolVersion.SSL_2) {
        SSLSocketFactory sslSocketFactory = factory(version, trustManager);
        factoryMap.put(version, sslSocketFactory);
      }
    }
    this.ssl2Client = SSL2Client.withAllKnownCiphers();
  }

  public ProtocolScanResult scan(TlsProtocolVersion protocolVersion, String hostname) {
    return scan(protocolVersion, hostname, DEFAULT_PORT);
  }

  // This method is only used in test cases
  public ProtocolScanResult scan(TlsProtocolVersion protocolVersion, String hostname, int destinationPort) {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(hostname, destinationPort);
    return scanForProtocol(protocolVersion, inetSocketAddress);
  }


  public TlsCrawlResult scan (InetSocketAddress address) {
    ProtocolScanResult scanResult_1_3  = scanForProtocol(TlsProtocolVersion.TLS_1_3, address);
    if (!scanResult_1_3.isConnectOK()) {
      logger.debug("Could not connect to port {}, no need to check other TLS versions", address.getPort());
      return TlsCrawlResult.connectFailed(address, scanResult_1_3.getErrorMessage());
    }
    TlsCrawlResult crawlResult = new TlsCrawlResult(true);
    crawlResult.add(scanResult_1_3);
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_2, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_1, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_0, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.SSL_3, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.SSL_2, address));
    return crawlResult;
  }

  public ProtocolScanResult scanForSSL2(InetSocketAddress socketAddress) {
    if (socketAddress.getAddress() instanceof Inet6Address && !ipv6Enabled) {
      return SSL2ScanResult.failed(socketAddress, "Scanning IPv6 not enabled");
    }
    logger.info("Checking SSLv2 support on {}", socketAddress);
    // This will not do a full SSL handshake, just exchange ClientHello and ServerHello
    SSL2ScanResult scanResult = ssl2Client.connect(socketAddress);
    logger.debug("scanResult = {}", scanResult);
    return scanResult;
  }

  public ProtocolScanResult scanForProtocol(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    Instant start = Instant.now();
    ProtocolScanResult protocolScanResult = scanForProtocol_(protocolVersion, socketAddress);
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    protocolScanResult.setScanDuration(duration);
    logger.debug("Scanning {} for {} took {}", socketAddress, protocolVersion, duration);
    return protocolScanResult;
  }

  private ProtocolScanResult scanForProtocol_(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    logger.info("Checking {} for {} support", socketAddress, protocolVersion);
    if (protocolVersion == TlsProtocolVersion.SSL_2) {
      // JDK does not support SSL 2.0 => use our own code to exchange Client & Server Hello messages
      return scanForSSL2(socketAddress);
    }
    ProtocolScanResult scanResult = ProtocolScanResult.of(protocolVersion, socketAddress);
    if (socketAddress.isUnresolved()) {
      scanResult.setErrorMessage("Unknown host");
      return scanResult;
    }
    if (socketAddress.getAddress() instanceof Inet6Address && !ipv6Enabled) {
      scanResult.setErrorMessage("Scanning IPv6 not enabled");
      return scanResult;
    }
    SSLSocketFactory socketFactory = factoryMap.get(protocolVersion);

    try {
      Socket socketConn = new Socket();
      socketConn.connect(socketAddress, (int) connectTimeOut.toMillis());
      SSLSocket socket = (SSLSocket) socketFactory.createSocket(
          socketConn, socketAddress.getHostString(), socketAddress.getPort(), true);
      socket.setSoTimeout((int) readTimeOut.toMillis());
      scanResult.setConnectOK(true);
      scanResult.setIpAddress(socket.getInetAddress().getHostAddress());
      if (verbose) {
        logger.debug("socket.getRemoteSocketAddress = {}", socket.getRemoteSocketAddress());
      }
      try {
        String[] protocols = new String[]{protocolVersion.getName()};
        socket.setEnabledProtocols(protocols);
      } catch (IllegalArgumentException e) {
        logger.warn("Test of {} on {} => IllegalArgumentException: {}", protocolVersion, socketAddress, e.getMessage());
        logger.warn("IllegalArgumentException", e);
        scanResult.setErrorMessage(e.getMessage());
        return scanResult;
      }
      startHandshake(socket, protocolVersion, scanResult);

    } catch (SocketTimeoutException e) {
      scanResult.setConnectOK(false);
      scanResult.setErrorMessage(ProtocolScanResult.CONNECTION_TIMED_OUT);
    } catch (IOException e) {
      scanResult.setConnectOK(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("IOException while scanning {}", scanResult.getServerName());
      logger.info("IOException while scanning: {}", e.getMessage());
    }
    return scanResult;
  }

  private void log(String message, Object... args) {
    if (verbose) {
      logger.info(message, args);
    }
  }

  private void startHandshake(SSLSocket socket, TlsProtocolVersion protocolVersion, ProtocolScanResult scanResult) {
    try {
      socket.startHandshake();
      SSLSession sslSession = socket.getSession();
      log("sslSession.protocol      = {}", sslSession.getProtocol());
      log("sslSession.cipherSuite   = {}", sslSession.getCipherSuite());
      scanResult.setHandshakeOK(true);
      scanResult.setSelectedCipherSuite(sslSession.getCipherSuite());
      scanResult.setSelectedProtocol(sslSession.getProtocol());
      processCertificate(socket, scanResult);

    } catch (IOException e) {
      scanResult.setHandshakeOK(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("{} => {} : {}", protocolVersion, e.getClass().getSimpleName(), e.getMessage());
    }
  }

  private void processCertificate(SSLSocket socket, ProtocolScanResult scanResult) {
    SSLSession sslSession = socket.getSession();
    try {
      logger.info("sslSession.peerPrincipal = {}", sslSession.getPeerPrincipal());
      scanResult.setPeerVerified(true);
      scanResult.setPeerPrincipal(sslSession.getPeerPrincipal().getName());
      // an ordered array of peer certificates, with the peer's own certificate first followed by any certificate authorities.
      Certificate[] certificates = sslSession.getPeerCertificates();

      log("Found {} certificates", certificates.length);
      List<CertificateInfo> certificateChain = new ArrayList<>();
      boolean ok = true;
      int index = 0;
      CertificateInfo previous = null;
      for (Certificate certificate : certificates) {
        if (certificate instanceof X509Certificate x509Certificate) {
          CertificateInfo certificateInfo = CertificateInfo.from(x509Certificate);
          // here we assume that the certificates we got from sslSession.getPeerCertificates() form a chain
          // (1st entry is signed by 2nd entry etc)
          // A few lines lower we check if the chain is trusted by the java platform (with built-in set of trust anchors)
          if (previous != null) {
            previous.setSignedBy(certificateInfo);
          }
          previous = certificateInfo;
          certificateChain.add(certificateInfo);
          if (index == 0) {
            scanResult.setPeerCertificate(certificateInfo);
          }
          index++;
        } else {
          logger.warn("Found certificate of type {}, class={}", certificate.getType(), certificate.getClass());
          ok = false;
        }
        if (ok) {
          scanResult.setCertificateChain(certificateChain);
        }
      }
      boolean trusted = isChainTrustedByJavaPlatform(certificates);
      scanResult.setChainTrustedByJavaPlatform(trusted);

      boolean hostNameMatchesCertificate = hostnameVerifier.verify(scanResult.getServerName(), sslSession);
      scanResult.setHostNameMatchesCertificate(hostNameMatchesCertificate);

      logger.debug("Chain trusted: {} hostNameMatchesCertificate: {}", trusted, hostNameMatchesCertificate);

    } catch (SSLPeerUnverifiedException e) {
      scanResult.setPeerVerified(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("{} => SSLPeerUnverifiedException: {}", scanResult.getServerName(), e.getMessage());
    } catch (CertificateParsingException e) {
      scanResult.setPeerVerified(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("{} => CertificateParsingException: {}", scanResult.getServerName(), e.getMessage());
    }
  }

  private boolean isChainTrustedByJavaPlatform(Certificate[] certificates) {
    X509Certificate[] certs = (X509Certificate[]) certificates;
    try {
      Trust.defaultTrustManager().checkServerTrusted(certs, "UNKNOWN");
      logger.debug("Chain for {} IS trusted by Java platform", certs[0].getSubjectX500Principal());
      return true;
    } catch (CertificateException e) {
      logger.debug("Chain for {} NOT trusted by Java platform: {}", certs[0].getSubjectX500Principal(), e.getMessage());
      return false;
    }
  }

  public static SSLSocketFactory factory(TlsProtocolVersion tlsProtocolVersion, TrustManager trustManager)
      throws NoSuchAlgorithmException, KeyManagementException {
    return factory(tlsProtocolVersion, trustManager, false);
  }

  public static SSLSocketFactory factory(TlsProtocolVersion tlsProtocolVersion, TrustManager trustManager, boolean verbose) throws NoSuchAlgorithmException, KeyManagementException {
    String clsName = java.security.Security.getProperty("ssl.SocketFactory.provider");
    if (clsName != null) {
      logger.info("ssl.SocketFactory.provider = {}", clsName);
    }
    TrustManager[] trustManagers = new TrustManager[] { trustManager };
    SSLContext sslContext = SSLContext.getInstance(tlsProtocolVersion.getName());
    sslContext.init(null, trustManagers, null);
    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
    if (verbose) {
      logSupportedCipherSuites(tlsProtocolVersion, sslSocketFactory);
    }
    return sslSocketFactory;
  }

  private static void logSupportedCipherSuites(TlsProtocolVersion protocolVersion, SSLSocketFactory sslSocketFactory) {
    String[] supportedCiphers = sslSocketFactory.getSupportedCipherSuites();
    logger.info("sslSocketFactory for {} supports {} ciphers", protocolVersion, supportedCiphers.length);
    for (String cipher : supportedCiphers) {
      logger.debug("* supported cipherSuites for {} : {}", protocolVersion, cipher);
    }
    String[] defaultCiphers = sslSocketFactory.getDefaultCipherSuites();
    logger.info("sslSocketFactory for {} has {} default ciphers", protocolVersion, defaultCiphers.length);
    for (String cipher : defaultCiphers) {
      logger.debug("* default cipherSuites for {} : {}", protocolVersion, cipher);
    }
  }

}
