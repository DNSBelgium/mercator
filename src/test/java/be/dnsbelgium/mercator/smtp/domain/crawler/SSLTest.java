package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/*
 * This test doesn't do much yet. It's mainly preparation in case we want to check TLS versions / ciphers supported by SMTP servers
 * and/or the validity of self-signed certs in combination with TLSA records
 */
public class SSLTest {

    private static final Logger logger = getLogger(SSLTest.class);

    // https://docs.oracle.com/javase/9/docs/specs/security/standard-names.html

    @Test
    public void all() {
        printSSLContext("SSL");
        printSSLContext("SSLv2");
        printSSLContext("SSLv3");
        printSSLContext("TLS");
        printSSLContext("TLSv1");
        printSSLContext("TLSv1.1");
        printSSLContext("TLSv1.2");
        printSSLContext("TLSv1.3");
    }

    private void printSSLContext(String protocol) {
        try {
            print(SSLContext.getInstance(protocol));
        } catch (NoSuchAlgorithmException e) {
            logger.info("{} => {}", protocol, e.getLocalizedMessage());
        }
    }


    @Test
    public void tls1_1() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
        print(sslContext);
    }

    @Test
    public void tls1_2() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        print(sslContext);
    }

    @Test
    public void tls1_3() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        print(sslContext);
    }

    @Test
    public void the_default() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getDefault();
        print(sslContext);
    }

    private void print(SSLContext sslContext) {
        try {
            sslContext.init(null, null, null);
        } catch (KeyManagementException e) {
            logger.info("e = {}", e.getMessage());
        }
        sslContext.createSSLEngine();
        logger.info("sslContext.getProtocol() = {}", sslContext.getProtocol());
        print("getCipherSuites() = {}", sslContext.getSupportedSSLParameters().getCipherSuites());
        String[] defaultCipherSuites = sslContext.getServerSocketFactory().getDefaultCipherSuites();
        print("defaultCipherSuites   = {}", defaultCipherSuites);
        String[] supportedCipherSuites = sslContext.getServerSocketFactory().getSupportedCipherSuites();
        print("supportedCipherSuites = {}", supportedCipherSuites);
    }


    private void print(String label, String[] strings) {
        logger.info("{} : {} items", label, strings.length);
        logger.info("{} = {}", label, Arrays.toString(strings));
    }

    @Test
    public void testLoadKeyStores() {
        KeyManager[] keyManagers = loadKeyStore().getKeyManagers();
        logger.info("keyManagers = {}", keyManagers.length);
        assertThat(keyManagers.length).isGreaterThan(0);
        TrustManager[] trustManagers = loadTrustManagerFactory().getTrustManagers();
        logger.info("trustManagers = {}", trustManagers.length);
        assertThat(trustManagers.length).isGreaterThan(0);
    }

    public static KeyManagerFactory loadKeyStore() {
        char[] keyStorePassphrase = "password".toCharArray();
        try {
            InputStream keyStoreResource = SSLTest.class.getResourceAsStream("/keyStore.jks");
            KeyStore ksKeys = KeyStore.getInstance("PKCS12");
            ksKeys.load(keyStoreResource, keyStorePassphrase);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, keyStorePassphrase);
            logger.info("kmf.algorithm = " + kmf.getAlgorithm());
            logger.info("key managers = " + Arrays.toString(kmf.getKeyManagers()));
            logger.info("key manager info = " + kmf.getProvider().getInfo());
            return kmf;
        } catch (IOException | CertificateException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }


    public static TrustManagerFactory loadTrustManagerFactory() {
        // Trust store contains certificates of trusted certificate authorities.
        // Needed for client certificate validation.
        try {
            InputStream trustStoreStream = SSLTest.class.getResourceAsStream("/trustStore.jks");
            char[] trustStorePassphrase = "password".toCharArray();
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(trustStoreStream, trustStorePassphrase);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);
            logger.info("tmf algorithm = " + tmf.getAlgorithm());
            logger.info("tmf trust managers = " + Arrays.toString(tmf.getTrustManagers()));
            logger.info("tmf provider info = " + tmf.getProvider().getInfo());
            return tmf;
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
