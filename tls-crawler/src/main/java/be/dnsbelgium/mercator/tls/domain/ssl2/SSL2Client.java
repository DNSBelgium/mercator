package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.ProtocolScanResult;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;

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

  public SSL2ScanResult connect(String host) throws InterruptedException {
    return connect(host, 443);
  }
  public SSL2ScanResult connect(String hostName, int port) {
    InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
    return connect(socketAddress);
  }

  public SSL2ScanResult connect(InetSocketAddress socketAddress) {
    ClientHandler clientHandler = new ClientHandler(socketAddress, cipherSuites);
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);

    Instant start = Instant.now();
    try {
      Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, false);
      b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);

      b.handler(new ChannelInitializer<SocketChannel>() {
        @SuppressWarnings("NullableProblems")
        @Override
        public void initChannel(SocketChannel ch) {
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

      Instant end = Instant.now();
      Duration duration = Duration.between(start, end);
      logger.info("duration: {} ms", duration.toMillis());

      if (connectFuture.isSuccess()) {
        return clientHandler.scanResult();
      }
      return failure(socketAddress, connectFuture.cause());

    } catch (Exception e) {
      logger.debug("We don't expect to get here. e.class={}", e.getClass());
      return failure(socketAddress, e);
    } finally {
      workerGroup.shutdownGracefully();
    }
 }

 private static SSL2ScanResult failure(InetSocketAddress socketAddress, Throwable cause) {
   String errorMessage = errorMessageFrom(cause);
   return SSL2ScanResult.failed(socketAddress, errorMessage);
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
      return ProtocolScanResult.CONNECTION_TIMED_OUT;
    }
    if (causeMsg.contains("connection refused")) {
      return ProtocolScanResult.CONNECTION_REFUSED;
    }
    return cause.getMessage();
 }

}
