package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CodecException;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class ServerHelloDecoder extends ByteToMessageDecoder {

  private static final Logger logger = getLogger(ServerHelloDecoder.class);

  public ServerHelloDecoder() {
  }

  /*
  5.6.1 SERVER-HELLO (Phase 1; Sent in the clear)

  char MSG-SERVER-HELLO
  char SESSION-ID-HIT
  char CERTIFICATE-TYPE
  char SERVER-VERSION-MSB
  char SERVER-VERSION-LSB
  char CERTIFICATE-LENGTH-MSB
  char CERTIFICATE-LENGTH-LSB
  char CIPHER-SPECS-LENGTH-MSB
  char CIPHER-SPECS-LENGTH-LSB
  char CONNECTION-ID-LENGTH-MSB
  char CONNECTION-ID-LENGTH-LSB
  char CERTIFICATE-DATA[MSB<<8|LSB]
  char CIPHER-SPECS-DATA[MSB<<8|LSB]
  char CONNECTION-ID-DATA[MSB<<8|LSB]

  CERTIFICATE-TYPE is one of:  SSL_X509_CERTIFICATE
  The CERTIFICATE-DATA contains an X.509 (1988) [3] signed certificate.
  */


  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

    // TLS 1.2 servers should not support SSL 2.0 and could (or SHOULD ?) send an Alert message
    // instead of a ServerHello when receiving an SSL 2.0 ClientHello

    // when connecting to www.dnsbelgium.be the alert message is 7 bytes long:  // 15 03 03 00 02 02 46  => TLS 1.2
    // when connecting to www.sidn.nl       the alert message is 7 bytes long:  // 15 03 01 00 02 02 46  => TLS 1.0
    //
    // content-type : 0x15        => alert
    // version      : 0x03 0x03   => TLS 1.2
    // length       : 0x00 0x02   => 2 more bytes
    // alert level  : 0x00        => Level: FATAL
    // alert desc   : 0x46        => Description: Protocol Version (70)
    //
    // The server will then close the socket

    logger.info("decoding ServerHello coming from ctx = {}", ctx);
    if (TlsDecoder.incompleteData(in)) {
      return;
    }
    int msgType = in.readByte();
    logger.info("msgType = {}", msgType);
    if (msgType != ServerHelloEncoder.SERVER_HELLO_MESSAGE_TYPE) {
      logger.error("Incoming message is not a ServerHello! msgType = {}", msgType);
      throw new CodecException("Incoming message is not a ServerHello! msgType=" + msgType);
    }
    boolean sessionIdHit = in.readBoolean();
    int certificateType = in.readByte();
    byte[] versionSelectedByServer = new byte[] { in.readByte(), in.readByte() };

    int certificateLength   = in.readUnsignedShort();
    int cipherSpecLength   = in.readUnsignedShort();
    int connectionIdLength = in.readUnsignedShort();

    logger.info("certificateLength = {}", certificateLength);
    logger.info("cipherSpecLength = {}", cipherSpecLength);
    logger.info("connectionIdLength = {}", connectionIdLength);

    byte[] certificate = new byte[certificateLength];
    in.readBytes(certificate);
    logger.debug("We have read {} bytes of the certificate", certificateLength);

    byte[] cipherSpecs = new byte[cipherSpecLength];
    in.readBytes(cipherSpecs);
    List<SSL2CipherSuite> listSupportedCipherSuites = SSL2CipherSuite.getCipherSuites(cipherSpecs);

    byte[] connectionId = new byte[connectionIdLength];
    in.readBytes(connectionId);
    logger.debug("We have read {} bytes of the connectionId", connectionIdLength);

    ServerHello serverHello = new ServerHello(
        sessionIdHit,
        certificateType,
        versionSelectedByServer,
        certificate,
        listSupportedCipherSuites,
        connectionId
    );
    logger.info("Decoded serverHello = {}", serverHello);
    out.add(serverHello);
  }

}
