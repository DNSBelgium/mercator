package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class ServerHelloEncoder extends MessageToByteEncoder<ServerHello> {

  public final static int SERVER_HELLO_MESSAGE_TYPE = 4;

  private static final Logger logger = getLogger(ServerHelloEncoder.class);

  /*
  Transforms a ServerHello into the wire representation
   */
  @Override
  protected void encode(ChannelHandlerContext ctx, ServerHello serverHello, ByteBuf out) {
    logger.info("Encoding a ServerHello message, out.writableBytes() = {}", out.writableBytes());
    int tlsRecordLength = bytesNeeded(serverHello);
    logger.debug("tlsRecordLength for ServerHello: {}", tlsRecordLength);
    //out.writeShort(tlsRecordLength);

    TlsDecoder.write(new TlsRecordHeader(tlsRecordLength), out);

    out.writeByte(SERVER_HELLO_MESSAGE_TYPE);
    out.writeBoolean(serverHello.isSessionIdHit());
    out.writeByte(serverHello.getCertificateType());
    out.writeBytes(serverHello.getVersionSelectedByServer(),0,2);
    out.writeShort(serverHello.getCertificate().length);
    out.writeShort(serverHello.getCipherSpecLength());
    out.writeShort(serverHello.getConnectionId().length);
    out.writeBytes(serverHello.getCertificate());
    for (SSL2CipherSuite supportedCipherSuite : serverHello.getListSupportedCipherSuites()) {
      out.writeBytes(supportedCipherSuite.getByteValue());
    }
    out.writeBytes(serverHello.getConnectionId());
    logger.info("DONE encoding a ServerHello message, out.writableBytes() = {}", out.writableBytes());
  }

  public int bytesNeeded(ServerHello serverHello) {
    return 1 // message type
        + 1  // sessionId Hit
        + 1  // certificate type
        + 2  // version selected by server
        + 2  // certificate length
        + 2  //  Cipher Spec length
        + 2  //  Connection Id length
        + serverHello.getCertificate().length
        + 3 * serverHello.getListSupportedCipherSuites().size()
        + serverHello.getConnectionId().length;
  }

}
