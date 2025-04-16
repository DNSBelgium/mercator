package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import be.dnsbelgium.mercator.tls.domain.certificates.Trust;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2Client;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2Scan;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.tls.metrics.MetricName.*;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsScanner {

  private static final Logger logger = getLogger(TlsScanner.class);
  private static final int DEFAULT_PORT = 443;
  public static final String ERROR_UNKNOWN_HOST = "Unknown host";
  public static final String IPV6_NOT_ENABLED = "Scanning IPv6 not enabled";

  private final Map<TlsProtocolVersion, SSLSocketFactory> factoryMap = new HashMap<>();

  private final boolean ipv6Enabled;

  private final boolean verbose;

  private final Duration connectTimeOut;
  private final Duration readTimeOut;

  private final SSL2Client ssl2Client;

  private final HostnameVerifier hostnameVerifier;

  public final static int DEFAULT_CONNECT_TIME_OUT_MS = 5000;
  public final static int DEFAULT_READ_TIME_OUT_MS = 5000;
  private final MeterRegistry meterRegistry;

  private final RateLimiter rateLimiter;

  /**
   * This method needs to be called as soon as possible after the start of the JVM
   * if we do this early enough, we don't have to set a system property when starting the JVM
   * (-Djava.security.properties=/path/to/custom/security.properties)
   */
  public static void allowOldAlgorithms() {
    logger.debug("setting security property \"jdk.tls.disabledAlgorithms\" to \"NULL\"");
    Security.setProperty("jdk.tls.disabledAlgorithms", "NULL");
  }


  public static TlsScanner standard(RateLimiter rateLimiter) {
    return new TlsScanner(
        new DefaultHostnameVerifier(),
        rateLimiter,
        new SimpleMeterRegistry(),
        false, false, DEFAULT_CONNECT_TIME_OUT_MS, DEFAULT_READ_TIME_OUT_MS);
  }

  @SneakyThrows
  @Autowired
  public TlsScanner(
      HostnameVerifier hostnameVerifier,
      RateLimiter rateLimiter,
      MeterRegistry meterRegistry,
      @Value("${tls.scanner.ipv6.enabled:false}") boolean ipv6Enabled,
      @Value("${tls.scanner.verbose:false}") boolean verbose,
      @Value("${tls.scanner.connect.timeout.milliSeconds:3000}") int connectTimeOutMilliSeconds,
      @Value("${tls.scanner.read.timeout.milliSeconds:3000}") int readTimeOutMilliSeconds) {
    this.hostnameVerifier = hostnameVerifier;
    this.meterRegistry = meterRegistry;
    this.rateLimiter = rateLimiter;
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

  public SingleVersionScan scan(TlsProtocolVersion protocolVersion, String hostname) {
    return scan(protocolVersion, hostname, DEFAULT_PORT);
  }

  // This method is only used in test cases
  public SingleVersionScan scan(TlsProtocolVersion protocolVersion, String hostname, int destinationPort) {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(hostname, destinationPort);
    return scanForProtocol(protocolVersion, inetSocketAddress);
  }


  public FullScan scan(InetSocketAddress address) {
    SingleVersionScan scan_1_3 = scanForProtocol(TlsProtocolVersion.TLS_1_3, address);
    if (!scan_1_3.isConnectOK()) {
      logger.debug("Could not connect to port {}, no need to check other TLS versions", address.getPort());
      return FullScan.connectFailed(address, scan_1_3.getErrorMessage());
    }
    FullScan crawlResult = new FullScan(true);
    crawlResult.add(scan_1_3);
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_2, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_1, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.TLS_1_0, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.SSL_3, address));
    crawlResult.add(scanForProtocol(TlsProtocolVersion.SSL_2, address));
    return crawlResult;
  }

  public SingleVersionScan scanForSSL2(InetSocketAddress socketAddress) {
    if (socketAddress.getAddress() instanceof Inet6Address && !ipv6Enabled) {
      return SSL2Scan.failed(socketAddress, "Scanning IPv6 not enabled");
    }
    logger.info("Checking SSLv2 support on {}", socketAddress);
    // This will not do a full SSL handshake, just exchange ClientHello and ServerHello
    SingleVersionScan scan = ssl2Client.connect(socketAddress);
    logger.debug("SSL2Scan = {}", scan);
    return scan;
  }

  public SingleVersionScan scanForProtocol(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    rateLimiter.sleepIfNecessaryFor(socketAddress);
    Instant start = Instant.now();
    SingleVersionScan singleVersionScan = scanForProtocol_(protocolVersion, socketAddress);
    Duration duration = Duration.between(start, Instant.now());
    singleVersionScan.setScanDuration(duration);
    logger.debug("Scanning {} for {} took {}", socketAddress, protocolVersion, duration);
    rateLimiter.registerDuration(socketAddress, duration);
    return singleVersionScan;
  }

  private SingleVersionScan scanForProtocol_(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
    logger.debug("Checking {} for {} support", socketAddress, protocolVersion);

    if (protocolVersion == TlsProtocolVersion.SSL_2) {
      // JDK does not support SSL 2.0 => use our own code to exchange Client & Server Hello messages
      return scanForSSL2(socketAddress);
    }
    SingleVersionScan singleVersionScan = SingleVersionScan.of(protocolVersion, socketAddress);
    if (socketAddress.isUnresolved()) {
      singleVersionScan.setErrorMessage(ERROR_UNKNOWN_HOST);
      return singleVersionScan;
    }
    if (socketAddress.getAddress() instanceof Inet6Address && !ipv6Enabled) {
      singleVersionScan.setErrorMessage(IPV6_NOT_ENABLED);
      return singleVersionScan;
    }
    SSLSocketFactory socketFactory = factoryMap.get(protocolVersion);

    try {
      Socket socketConn = new Socket();
      socketConn.connect(socketAddress, (int) connectTimeOut.toMillis());
      SSLSocket socket = (SSLSocket) socketFactory.createSocket(
          socketConn, socketAddress.getHostString(), socketAddress.getPort(), true);
      socket.setSoTimeout((int) readTimeOut.toMillis());
      singleVersionScan.setConnectOK(true);
      singleVersionScan.setIpAddress(socket.getInetAddress().getHostAddress());
      if (verbose) {
        logger.debug("socket.getRemoteSocketAddress = {}", socket.getRemoteSocketAddress());
      }
      try {
        String[] protocols = new String[]{protocolVersion.getName()};
        socket.setEnabledProtocols(protocols);
      } catch (IllegalArgumentException e) {
        logger.warn("Test of {} on {} => IllegalArgumentException: {}", protocolVersion, socketAddress, e.getMessage());
        logger.warn("Probably the JVM is not correctly configured? IllegalArgumentException", e);
        singleVersionScan.setErrorMessage(e.getMessage());
        return singleVersionScan;
      }
      startHandshake(socket, protocolVersion, singleVersionScan);

    } catch (SocketTimeoutException e) {
      singleVersionScan.setConnectOK(false);
      singleVersionScan.setErrorMessage(SingleVersionScan.CONNECTION_TIMED_OUT);
      meterRegistry.counter(COUNTER_CONNECTION_TIMEOUTS, "version", protocolVersion.getName()).increment();
    } catch (IOException e) {
      singleVersionScan.setConnectOK(false);
      singleVersionScan.setErrorMessage(e.getMessage());
      logger.debug("IOException while scanning {}", singleVersionScan.getServerName());
      logger.debug("IOException while scanning: {}", e.getMessage());
      meterRegistry.counter(COUNTER_IO_EXCEPTIONS, "version", protocolVersion.getName()).increment();
    }
    return singleVersionScan;
  }

  private void log(String message, Object... args) {
    if (verbose) {
      logger.info(message, args);
    }
  }

  private void startHandshake(SSLSocket socket, TlsProtocolVersion protocolVersion, SingleVersionScan singleVersionScan) {
    try {
      socket.startHandshake();
      SSLSession sslSession = socket.getSession();
      log("sslSession.protocol      = {}", sslSession.getProtocol());
      log("sslSession.cipherSuite   = {}", sslSession.getCipherSuite());
      singleVersionScan.setHandshakeOK(true);
      singleVersionScan.setSelectedCipherSuite(sslSession.getCipherSuite());
      singleVersionScan.setSelectedProtocol(sslSession.getProtocol());
      processCertificate(socket, singleVersionScan);

    } catch (IOException e) {
      singleVersionScan.setHandshakeOK(false);
      singleVersionScan.setErrorMessage(e.getMessage());
      meterRegistry.counter(COUNTER_HANDSHAKE_FAILURES, "version", protocolVersion.getName()).increment();
      logger.debug("{} => {} : {}", protocolVersion, e.getClass().getSimpleName(), e.getMessage());
    }
  }

  private void processCertificate(SSLSocket socket, SingleVersionScan singleVersionScan) {
    SSLSession sslSession = socket.getSession();
    try {
      logger.debug("sslSession.peerPrincipal = {}", sslSession.getPeerPrincipal());
      singleVersionScan.setPeerVerified(true);
      singleVersionScan.setPeerPrincipal(sslSession.getPeerPrincipal().getName());
      // an ordered array of peer certificates, with the peer's own certificate first followed by any certificate authorities.
      java.security.cert.Certificate[] certificates = sslSession.getPeerCertificates();

      log("Found {} certificates", certificates.length);
      List<Certificate> certificateChain = new ArrayList<>();
      boolean ok = true;
      int index = 0;
      Certificate previous = null;
      for (java.security.cert.Certificate cert: certificates) {
        if (cert instanceof X509Certificate x509Certificate) {
          Certificate certificate = Certificate.from(x509Certificate);
          // here we assume that the certificates we got from sslSession.getPeerCertificates() form a chain
          // (1st entry is signed by 2nd entry etc)
          // A few lines lower we check if the chain is trusted by the java platform (with built-in set of trust anchors)
          if (previous != null) {
            previous.setSignedBy(certificate);
          }
          previous = certificate;
          certificateChain.add(certificate);
          if (index == 0) {
            singleVersionScan.setPeerCertificate(certificate);
          }
          index++;
        } else {
          logger.warn("Found certificate of type {}, class={}", cert.getType(), cert.getClass());
          ok = false;
        }
      }
      if (ok) {
        singleVersionScan.setCertificateChain(certificateChain);
      }
      boolean trusted = isChainTrustedByJavaPlatform(certificates);
      singleVersionScan.setChainTrustedByJavaPlatform(trusted);

      boolean hostNameMatchesCertificate = hostnameVerifier.verify(singleVersionScan.getServerName(), sslSession);
      singleVersionScan.setHostNameMatchesCertificate(hostNameMatchesCertificate);

      logger.debug("Chain trusted: {} hostNameMatchesCertificate: {}", trusted, hostNameMatchesCertificate);

    } catch (SSLPeerUnverifiedException e) {
      singleVersionScan.setPeerVerified(false);
      singleVersionScan.setErrorMessage(e.getMessage());
      logger.debug("{} => SSLPeerUnverifiedException: {}", singleVersionScan.getServerName(), e.getMessage());
    } catch (CertificateParsingException e) {
      singleVersionScan.setPeerVerified(false);
      singleVersionScan.setErrorMessage(e.getMessage());
      logger.debug("{} => CertificateParsingException: {}", singleVersionScan.getServerName(), e.getMessage());
      meterRegistry.counter(COUNTER_CERTIFICATE_PARSING_ERRORS).increment();
    }
  }

  private boolean isChainTrustedByJavaPlatform(java.security.cert.Certificate[] certificates) {
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
