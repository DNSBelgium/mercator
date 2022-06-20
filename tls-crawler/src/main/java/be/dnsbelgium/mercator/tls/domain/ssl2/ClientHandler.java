package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import io.netty.channel.*;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ClientHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = getLogger(ClientHandler.class);
  private static final byte[] NO_SESSION_ID = new byte[0];

  private final List<SSL2CipherSuite> cipherSuites;

  private boolean connectOK = false;
  private ServerHello serverHello;

  private String errorMessage;

  private final InetSocketAddress socketAddress;

  public ClientHandler(InetSocketAddress socketAddress, List<SSL2CipherSuite> cipherSuites) {
    this.socketAddress = socketAddress;
    this.cipherSuites = cipherSuites;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    logger.debug("Connected to {}", ctx.channel().remoteAddress());
    this.connectOK = true;
    byte[] challenge = RandomUtils.nextBytes(32);
    ClientHello clientHello = new ClientHello(
        2,
        cipherSuites,
        NO_SESSION_ID,
        challenge
    );
    logger.debug("Writing clientHello = {}", clientHello);
    ctx.channel().writeAndFlush(clientHello);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    logger.debug("{} => exceptionCaught: {}", ctx.channel().remoteAddress(), cause.getMessage());
    this.errorMessage = cause.getMessage();
    if (ctx.channel() != null) {
      logger.info("Closing the connection");
      ctx.channel().close();
    }
  }


  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    logger.debug("channelRead");
    if (msg instanceof ServerHello) {
      this.serverHello = (ServerHello) msg;
      logger.debug("Received ServerHello: {}", serverHello);
    } else {
      this.errorMessage = "Unexpected response";
    }
    if (ctx != null) {
      logger.info("Closing the connection");
      ctx.channel().close();
    }
  }

  public SSL2ScanResult scanResult() {
    SSL2ScanResult scanResult = new SSL2ScanResult();
    scanResult.setAddress(socketAddress);
    scanResult.setProtocolVersion(TlsProtocolVersion.SSL_2);
    scanResult.setConnectOK(this.connectOK);
    scanResult.setPeerVerified(false);
    scanResult.setServerHello(serverHello);
    if (this.serverHello != null) {
      scanResult.setSelectedProtocol(serverHello.selectedVersion());
      // Consider first in the list in ServerHello as the selected cipher
      if (!serverHello.getListSupportedCipherSuites().isEmpty()) {
        scanResult.setSelectedCipherSuite(serverHello.getListSupportedCipherSuites().get(0).name());
      }
      scanResult.setHandshakeOK(true);
      scanResult.setErrorMessage(null);
    } else {
      scanResult.setHandshakeOK(false);
      scanResult.setErrorMessage(errorMessage);
    }
    return scanResult;
  }

}
