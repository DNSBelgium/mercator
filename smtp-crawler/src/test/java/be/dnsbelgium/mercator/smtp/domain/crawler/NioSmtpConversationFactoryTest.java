package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static org.slf4j.LoggerFactory.getLogger;

// TODO: find a good way to test various SMTP server behaviours
// TODO: move commented tests to other test class since they not testing the factory itself
class NioSmtpConversationFactoryTest {

    private static final Logger logger = getLogger(NioSmtpConversationFactoryTest.class);

    // This test can be used to debug a real SMTP conversation with a specific IP
    @Test
    public void longerTimeout() throws KeyManagementException, NoSuchAlgorithmException, ExecutionException, InterruptedException {

        SmtpConfig config = SmtpConfig.testConfig();

        NioSmtpConversationFactory factory = new NioSmtpConversationFactory(new SimpleMeterRegistry(), config);
        // this IP of "cavin.kuleuven.be" takes around 5.900 before accepting a connection
        NioSmtpConversation conversation = factory.create(ip("134.58.240.3"));
        logger.info("conversation = {}", conversation);
        long start = System.currentTimeMillis();
        CompletableFuture<SmtpHostIp> result = conversation.start();
        logger.info("result = {}", result);
        logger.info("waiting until done ...");
        SmtpHostIp smtpHostIp = result.get();
        long millis = System.currentTimeMillis() - start;
        logger.info("Conversation took {} ms", millis);
        logger.info("smtpHostIp = {}", smtpHostIp);
    }

    /*

    public final static String DNSBELGIUM_OUTLOOK_HOST     = "104.47.5.36";
    public final static InetAddress DNSBELGIUM_OUTLOOK_IP  = ip(DNSBELGIUM_OUTLOOK_HOST);

    private CompletableFuture<SmtpHostIp> future;

    // this test depends on external state !
    @Test
    public void crawlOneServer() throws ExecutionException, InterruptedException {
        future = crawlFactory.crawl(ip("104.47.5.36"));
        logger.info("future = {}", future);
        logger.info("result = {}", future.get());
    }

    @Test
    public void connectionRefused() throws ExecutionException, InterruptedException {
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        logger.info("result = {}", result);
        assertThat(result.getErrorMessage()).contains("Connection refused");
        assertThat(result.isConnectOK()).isFalse();
        assertThat(result.isStartTlsOk()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(0);
    }

    @Test
    public void testConnectionClosed() throws ExecutionException, InterruptedException {
        Executors.newSingleThreadExecutor().execute(this::startServer);
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        logger.info("result = {}", result);
        assertThat(result.getErrorMessage()).contains("channel was closed while waiting for response");
        assertThat(result.isConnectOK()).isFalse();
        assertThat(result.isStartTlsOk()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(0);
    }

    @Test
    public void testWeirdResultCode() throws ExecutionException, InterruptedException {
        Executors.newSingleThreadExecutor().execute(() -> serverReplies(500, "weird greeting"));
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        logger.info("result = {}", result);
        assertThat(result.getErrorMessage()).contains("Timed out waiting for a response to [EHLO smtp.crawler.dnsbelgium.be]");
        assertThat(result.isConnectOK()).isFalse();
        assertThat(result.isStartTlsOk()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(500);
    }

    @Test
    public void testResultCode200() throws ExecutionException, InterruptedException {
        Executors.newSingleThreadExecutor().execute(() -> serverReplies(200, "hi there"));
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        logger.info("result = {}", result);
        //assertThat(result.getErrorMessage()).contains("Timed out waiting for a response to [EHLO smtp.crawler.dnsbelgium.be]");
        assertThat(result.isConnectOK()).isTrue();
        assertThat(result.isStartTlsOk()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(200);
    }

    @Test
    public void testResultCode220() throws ExecutionException, InterruptedException {
        Executors.newSingleThreadExecutor().execute(() -> serverReplies(220, "hi there"));
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        logger.info("result = {}", result);
        //assertThat(result.getErrorMessage()).contains("Timed out waiting for a response to [EHLO smtp.crawler.dnsbelgium.be]");
        assertThat(result.isConnectOK()).isTrue();
        assertThat(result.isStartTlsOk()).isFalse();
        assertThat(result.getConnectReplyCode()).isEqualTo(220);
    }

    public void startServer() {
        try {
            ServerSocket server = new ServerSocket(25);
            Socket connection = server.accept();
            //connection.getOutputStream().write("go away".getBytes());
            //connection.getOutputStream().flush();
            connection.close();
        } catch (Exception ignore) {
        }
    }

    public void serverReplies(int resultCode, String data) {
        try {
            ServerSocket server = new ServerSocket(25);
            Socket connection = server.accept();
            connection.getOutputStream().write((resultCode + " " +  data + "\r\n").getBytes());
            connection.getOutputStream().flush();
            sleep(200);
            //connection.getOutputStream().write(("250 HE1EUR02FT060.mail.protection.outlook.com\r\n").getBytes());
            //connection.getOutputStream().flush();
            //connection.close();
            server.close();
        } catch (Exception e) {
            logger.error("Failed to start server", e);
        }

    }

    @Test
    public void subEthaSmtpServer() throws NoSuchAlgorithmException, ExecutionException, InterruptedException, KeyManagementException {
        int port = 25;
        SSLContext sslContext = SSLContext.getInstance("SSLv3");
        //sslContext.init(null, null, null);
        // Key store for your own private key and signing certificates
        KeyManagerFactory kmf = SSLTest.loadKeyStore();
        sslContext.init(kmf.getKeyManagers(), SSLTest.loadTrustManagerFactory().getTrustManagers(), null);
        SMTPServer server = new SMTPServer.Builder()
                .port(port)
                .connectionTimeout(1, TimeUnit.MINUTES)
                .backlog(100)
                .requireTLS(false)
                .enableTLS(true)
                .hideTLS(false)
                .hostName("smtp.server.com")
                .maxMessageSize(100)
                .maxConnections(2)
                .maxRecipients(2)
                .softwareName("Postfix")
                .startTlsSocketFactory(sslContext)
                .build();

        server.start();
        future = crawlFactory.crawl(ip("127.0.0.1"));
        SmtpHostIp result = future.get();
        System.out.println("result = " + result);
        server.stop();
    }


    @SuppressWarnings("SameParameterValue")
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.info("e = {}", e.getMessage());
        }
    }
    */
}
