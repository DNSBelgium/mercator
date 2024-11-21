package be.dnsbelgium.mercator.smtp.domain.crawler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.smtp.client.SmtpSessionConfig;
import com.hubspot.smtp.client.SmtpSessionFactory;
import com.hubspot.smtp.client.SmtpSessionFactoryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class NioSmtpConversationFactory implements SmtpConversationFactory {

  private static final Logger logger = getLogger(NioSmtpConversationFactory.class);

  private final SmtpSessionFactory sessionFactory;
  private final SmtpConfig config;
  private final MeterRegistry meterRegistry;

  public NioSmtpConversationFactory(MeterRegistry meterRegistry, SmtpConfig config) throws NoSuchAlgorithmException, KeyManagementException {
    logger.info("Initializing an SmtpConversationFactory with config={}", config);
    this.meterRegistry = meterRegistry;
    this.config = config;
    final SSLContext sslContext = getSslContext();
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("nio-smtpclient-%d").build();
    SmtpSessionFactoryConfig sessionFactoryConfig = SmtpSessionFactoryConfig.builder()
        .eventLoopGroup(new NioEventLoopGroup(config.getNumThreads(), threadFactory))
        .executor(Executors.newCachedThreadPool(threadFactory))
        .sslEngineSupplier(() -> {
          SSLEngine engine = sslContext.createSSLEngine();
          engine.setUseClientMode(true);
          return engine;
        })
        .build();
    this.sessionFactory = new SmtpSessionFactory(sessionFactoryConfig);
    logger.info("SmtpConfig.readTimeOut = {}", config.getReadTimeOut());
    logger.info("SmtpConfig.initialResponseTimeOut = {}", config.getInitialResponseTimeOut());
    logger.info("threadFactory and sessionFactoryConfig created");
  }

  private SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext;
    if (config.isTrustAnyone()) {
      logger.info("We are configured to trust anyone => use InsecureTrustManagerFactory");
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, InsecureTrustManagerFactory.INSTANCE.getTrustManagers(), null);
    } else {
      sslContext = SSLContext.getDefault();
      logger.info("trustAnyone=false => Using the default SSLContext: {}", sslContext);
    }
    return sslContext;
  }

  @Override
  public NioSmtpConversation create(InetAddress ip) {
    logger.debug("Creating an SmtpCrawl for ip {}", ip);
    SmtpSessionConfig sessionConfig = SmtpSessionConfig
        .builder()
        .remoteAddress(new InetSocketAddress(ip, config.getSmtpPort()))
        .readTimeout(config.getReadTimeOut())
        .initialResponseTimeout(config.getInitialResponseTimeOut())
        .connectionId(ip.getHostAddress())
        .build();
    return new NioSmtpConversation(meterRegistry, sessionFactory, sessionConfig, config, ip);
  }


}
