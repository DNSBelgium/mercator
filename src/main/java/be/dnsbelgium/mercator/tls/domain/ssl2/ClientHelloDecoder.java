package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CodecException;
import org.slf4j.Logger;

import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.ssl2.ClientHelloEncoder.CLIENT_HELLO_MESSAGE_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

public class ClientHelloDecoder extends ByteToMessageDecoder {

  private static final Logger logger = getLogger(ClientHelloDecoder.class);

    /*
      char MSG-CLIENT-HELLO
      char CLIENT-VERSION-MSB
      char CLIENT-VERSION-LSB
      char CIPHER-SPECS-LENGTH-MSB
      char CIPHER-SPECS-LENGTH-LSB
      char SESSION-ID-LENGTH-MSB
      char SESSION-ID-LENGTH-LSB
      char CHALLENGE-LENGTH-MSB
      char CHALLENGE-LENGTH-LSB
      char CIPHER-SPECS-DATA[(MSB<<8)|LSB]
      char SESSION-ID-DATA[(MSB<<8)|LSB]
      char CHALLENGE-DATA[(MSB<<8)|LSB]
  */

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws CodecException {
    if (TlsDecoder.incompleteData(in)) {
      return;
    }
    int msgType = in.readByte();
    logger.info("msgType = {}", msgType);

    if (msgType != CLIENT_HELLO_MESSAGE_TYPE) {
      logger.error("Incoming message is not a ClientHello! msgType = {}", msgType);
      throw new CodecException("Incoming message is not a ClientHello! msgType=" + msgType);
    }

    int version = in.readUnsignedShort();
    int cipherSpecLength = in.readUnsignedShort();
    int sessionIdLength  = in.readUnsignedShort();
    int challengeLength  = in.readUnsignedShort();

    byte[] cipherSpecs = new byte[cipherSpecLength];
    in.readBytes(cipherSpecs);
    List<SSL2CipherSuite> listSupportedCipherSuites = SSL2CipherSuite.getCipherSuites(cipherSpecs);

    byte[] sessionId = new byte[sessionIdLength];
    byte[] challenge = new byte[challengeLength];

    in.readBytes(sessionId);
    in.readBytes(challenge);

    ClientHello clientHello = new ClientHello(version, listSupportedCipherSuites, sessionId, challenge);
    logger.debug("Decoded clientHello = {}", clientHello);
    out.add(clientHello);
  }

}
