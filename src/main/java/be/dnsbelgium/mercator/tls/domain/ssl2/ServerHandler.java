package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ServerHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = getLogger(ServerHandler.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    MDC.put("domainName", ctx.channel().attr(SSL2Client.DOMAIN_NAME).get());
    MDC.put("visitId", ctx.channel().attr(SSL2Client.VISIT_ID).get());
    try {
      doChannelRead(ctx, msg);
    } finally {
      MDC.remove("domainName");
      MDC.remove("visitId");
    }
  }

  public void doChannelRead(ChannelHandlerContext ctx, Object msg) {
    logger.debug("received msg: {}", msg);
    if (msg instanceof ClientHello clientHello) {
      logger.debug("clientHello = {}", clientHello);

      byte[] version = new byte[] {0x00, 0x02};
      byte[] certificate = new byte[100];
      byte[] connectionId = new byte[0];

      List<SSL2CipherSuite> ciphers = List.of(SSL2CipherSuite.SSL_CK_DES_64_CBC_WITH_MD5, SSL2CipherSuite.SSL_CK_IDEA_128_CBC_WITH_MD5);
      ServerHello serverHello = new ServerHello(false, 1, version, certificate, ciphers, connectionId);

      logger.info("Replying with {}", serverHello);

      ChannelFuture future = ctx.writeAndFlush(serverHello);
      future.addListener(ChannelFutureListener.CLOSE);
      future.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

    } else {
      logger.warn("Incoming message is not a ClientHello but {}", msg.getClass().getName());
    }
  }
}
