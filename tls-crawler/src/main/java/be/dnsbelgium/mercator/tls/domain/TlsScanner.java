package be.dnsbelgium.mercator.tls.domain;

import be.dnsbelgium.mercator.tls.domain.certificates.CertificateInfo;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2Client;
import be.dnsbelgium.mercator.tls.domain.ssl2.SSL2ScanResult;
import lombok.SneakyThrows;
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class TlsScanner {

  private static final Logger logger = getLogger(TlsScanner.class);

  private final Map<TlsProtocolVersion, SSLSocketFactory> factoryMap = new HashMap<>();

  private final int destinationPort;

  private final boolean ipv6Enabled;

  private final boolean verbose;

  private final Duration connectTimeOut;

  private final SSL2Client ssl2Client;

  public final static int DEFAULT_CONNECT_TIME_OUT_MS = 5000;

  public static TlsScanner standard() {
    return new TlsScanner(443, false, false, DEFAULT_CONNECT_TIME_OUT_MS);
  }

  @SneakyThrows
  public TlsScanner(
      @Value("${tls.scanner.destination.port:443}") int destinationPort,
      @Value("${tls.scanner.ipv6.enabled:false}") boolean ipv6Enabled,
      @Value("${tls.scanner.verbose:false}") boolean verbose,
      @Value("${tls.scanner.connect.timeout.milliSeconds:3000}") int connectTimeOutMilliSeconds) {
    this.destinationPort = destinationPort;
    this.verbose = verbose;
    this.ipv6Enabled = ipv6Enabled;
    this.connectTimeOut = Duration.ofMillis(connectTimeOutMilliSeconds);
    logger.info("Creating TlsScanner with destinationPort={}", destinationPort);
    for (TlsProtocolVersion version : TlsProtocolVersion.values()) {
      // Modern java versions do not support SSL v2 => do not try to create SSLSocketFactory
      if (version != TlsProtocolVersion.SSL_2) {
        SSLSocketFactory sslSocketFactory = factory(version);
        factoryMap.put(version, sslSocketFactory);
      }
    }
    this.ssl2Client = SSL2Client.withAllKnownCiphers();
  }

  public ProtocolScanResult scan(TlsProtocolVersion protocolVersion, String hostname) {
    Instant start = Instant.now();
    ProtocolScanResult scanResult  = scanForProtocol(protocolVersion, hostname);
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    scanResult.setScanDuration(duration);
    logger.info("Scanning {} for {} took {}", hostname, protocolVersion, duration);
    return scanResult;
  }

  // TODO: -- TODO: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target

  public TlsCrawlResult scan(String hostname) {
    InetSocketAddress address = new InetSocketAddress(hostname, destinationPort);
    ProtocolScanResult scanResult_1_3  = scanForProtocol(TlsProtocolVersion.TLS_1_3, address);
    if (!scanResult_1_3.isConnectOK()) {
      logger.debug("Could not connect to port {}, no need to check other TLS versions", destinationPort);
      return TlsCrawlResult.connectFailed(address, scanResult_1_3.getErrorMessage());
    }
    logger.info("address.isUnresolved() = {}", address.isUnresolved());
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

  public ProtocolScanResult scanForProtocol(TlsProtocolVersion protocolVersion, String hostname) {
    InetSocketAddress inetSocketAddress = new InetSocketAddress(hostname, destinationPort);
    return scanForProtocol(protocolVersion, inetSocketAddress);
  }

  public ProtocolScanResult scanForProtocol(TlsProtocolVersion protocolVersion, InetSocketAddress socketAddress) {
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
      // Default time out seems to be 1 minute 15 seconds
      Socket socketConn = new Socket();
      socketConn.connect(socketAddress, (int) connectTimeOut.toMillis());
      SSLSocket socket = (SSLSocket) socketFactory.createSocket(socketConn, socketAddress.getHostString(), destinationPort, true);

      scanResult.setConnectOK(true);
      scanResult.setIpAddress(socket.getInetAddress().getHostAddress());
      if (verbose) {
        logger.debug("socket.getRemoteSocketAddress = {}", socket.getRemoteSocketAddress());
      }

      try {
        String[] protocols = new String[]{protocolVersion.getName()};
        socket.setEnabledProtocols(protocols);
        //socket.setEnabledCipherSuites(socketFactory.getSupportedCipherSuites());
        // preferLocalCipherSuites
      } catch (IllegalArgumentException e) {
        logger.warn("Test of {} on {}:{} => IllegalArgumentException: {}",
            protocolVersion, scanResult.getServerName(), destinationPort, e.getMessage());
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

  private void logServerNames(SSLSocket socket) {
    SSLParameters parameters = socket.getSSLParameters();
    List<SNIServerName> serverNames = parameters.getServerNames();
    if (serverNames.size() > 1) {
      logger.info("socket.getSSLParameters().getServerNames.size = {}", serverNames.size());
      for (SNIServerName serverName : serverNames) {
        logger.info(" * serverName = {}", serverName);
      }
    }
    socket.setSSLParameters(parameters);
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

    } catch (SSLHandshakeException e) {
      // // TODO: is there a good reason to have three separate catch clauses ?
      scanResult.setHandshakeOK(false);
      scanResult.setErrorMessage(e.getMessage());

      //logger.error("stacktrace", e);

      logger.info("{} => SSLHandshakeException: {}", protocolVersion, e.getMessage());
    } catch (SSLException e) {
      scanResult.setHandshakeOK(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("{} => SSLException: {}", protocolVersion, e.getMessage());
    } catch (IOException e) {
      scanResult.setHandshakeOK(false);
      scanResult.setErrorMessage(e.getMessage());
      logger.info("{} => IOException: {}", protocolVersion, e.getMessage());
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
        if (certificate instanceof X509Certificate) {
          X509Certificate x509Certificate = (X509Certificate) certificate;
          CertificateInfo certificateInfo = CertificateInfo.from(x509Certificate);
          // here we assume that the certificates we got from sslSession.getPeerCertificates()
          // form a chain (1st entry is signed by 2nd entry etc)
          // This might not be the case ?
          // TODO: should we call java.security.cert.Certificate.verify(java.security.PublicKey) ??
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


  public static SSLSocketFactory factory(TlsProtocolVersion tlsProtocolVersion) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    return factory(tlsProtocolVersion, false);

  }
  public static SSLSocketFactory factory(TlsProtocolVersion tlsProtocolVersion, boolean verbose) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    String clsName = java.security.Security.getProperty("ssl.SocketFactory.provider");
    if (clsName != null) {
      logger.info("ssl.SocketFactory.provider = {}", clsName);
    }
    TrustManager[] trustManagers = trustManagers();
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

  private static TrustManager[] trustManagers() throws NoSuchAlgorithmException, KeyStoreException {
    // TODO: check if this enough and which cases it will trust
    String algo = TrustManagerFactory.getDefaultAlgorithm();
    //logger.debug("TrustManagerFactory.getDefaultAlgorithm = {}", algo);
    TrustManagerFactory trustManagerFactory;
    try {
      trustManagerFactory = TrustManagerFactory.getInstance(algo);
      trustManagerFactory.init((KeyStore) null);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
        throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
      }
      return trustManagers;
    } catch (NoSuchAlgorithmException e) {
      logger.info("NoSuchAlgorithmException: {}", e.getMessage());
      throw e;
    } catch (KeyStoreException e) {
      logger.info("KeyStoreException: {}", e.getMessage());
      throw e;
    }
  }

}
