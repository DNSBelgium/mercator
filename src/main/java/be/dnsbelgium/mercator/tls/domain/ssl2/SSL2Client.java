package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class SSL2Client {

  private static final Logger logger = getLogger(SSL2Client.class);

  private final ClientHelloEncoder clientHelloEncoder;

  private final List<SSL2CipherSuite> cipherSuites;

  public final static AttributeKey<String> DOMAIN_NAME = AttributeKey.newInstance("domainName");
  public final static AttributeKey<String> VISIT_ID = AttributeKey.newInstance("visitId");

  public static SSL2Client withAllKnownCiphers() {
    return new SSL2Client(SSL2CipherSuite.validCiphers());
  }

  public static SSL2Client with(SSL2CipherSuite... cipherSuites) {
    return new SSL2Client(List.of(cipherSuites));
  }

  private SSL2Client(List<SSL2CipherSuite> cipherSuites) {
    this.cipherSuites = cipherSuites;
    this.clientHelloEncoder = new ClientHelloEncoder();
  }

  public SSL2Scan connect(String host) throws InterruptedException {
    return connect(host, 443);
  }
  public SSL2Scan connect(String hostName, int port) {
    InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
    return connect(socketAddress);
  }

  public SSL2Scan connect(InetSocketAddress socketAddress) {
    Instant start = Instant.now();
    SSL2Scan scan = doConnect(socketAddress);
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    logger.info("duration: {} ms", duration.toMillis());
    return scan;
  }

  public SSL2Scan doConnect(InetSocketAddress socketAddress) {
    ClientHandler clientHandler = new ClientHandler(socketAddress, cipherSuites);
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    try {
      Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, false);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);

      IdleStateHandler idleHandler = new IdleStateHandler(5, 5, 0);

      b.attr(DOMAIN_NAME, MDC.get("domainName"));
      b.attr(VISIT_ID, MDC.get("visitId"));

      b.handler(new ChannelInitializer<SocketChannel>() {
        @SuppressWarnings("NullableProblems")
        @Override
        public void initChannel(SocketChannel ch) {
          ch.pipeline().addLast("idleStateHandler", idleHandler);
          ch.pipeline().addLast(new ServerHelloDecoder());
          ch.pipeline().addLast(clientHelloEncoder);
          ch.pipeline().addLast(clientHandler);
        }
      });
      logger.info("Connecting to {}:{}", socketAddress.getHostString(), socketAddress.getPort());
      ChannelFuture connectFuture = b.connect(socketAddress);
      connectFuture.await(3, TimeUnit.SECONDS);
      // Wait until the connection is closed.
      connectFuture.channel().closeFuture().sync();

      if (connectFuture.isSuccess()) {
        return clientHandler.result();
      }
      return failure(socketAddress, connectFuture.cause());

    } catch (Exception e) {
      logger.debug("We don't expect to get here. e.class={}", e.getClass());
      return failure(socketAddress, e);
    } finally {
      workerGroup.shutdownGracefully();
    }
 }

 private static SSL2Scan failure(InetSocketAddress socketAddress, Throwable cause) {
   String errorMessage = errorMessageFrom(cause);
   return SSL2Scan.failed(socketAddress, errorMessage);
 }

 private static String errorMessageFrom(Throwable cause) {
    if (cause == null) {
      return "Unknown error";
    }
    if (cause.getMessage() == null) {
      logger.info("Unknown error: class={} msg={}", cause.getClass(), cause.getMessage());
      return "Unknown error";
    }
    String causeMsg = cause.getMessage().toLowerCase();
    if (causeMsg.contains("connection timed out")) {
      return SingleVersionScan.CONNECTION_TIMED_OUT;
    }
    if (causeMsg.contains("connection refused")) {
      return SingleVersionScan.CONNECTION_REFUSED;
    }
    return cause.getMessage();
 }

}
