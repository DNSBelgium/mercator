package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@ChannelHandler.Sharable
public class ClientHelloEncoder extends MessageToByteEncoder<ClientHello> {

  public final static int CLIENT_HELLO_MESSAGE_TYPE = 1;
  private static final Logger logger = getLogger(ClientHelloEncoder.class);

  @Override
  protected void encode(ChannelHandlerContext ctx, ClientHello clientHello, ByteBuf out) {
    int tlsRecordLength = bytesNeeded(clientHello);
    logger.debug("tlsRecordLength for ClientHello: {}", tlsRecordLength);
    TlsDecoder.write(new TlsRecordHeader(tlsRecordLength), out);

    out.writeByte(CLIENT_HELLO_MESSAGE_TYPE);
    out.writeShort(clientHello.getMaxSupportedVersion());
    out.writeShort(clientHello.getListSupportedCipherSuites().size() * 3);
    out.writeShort(clientHello.getSessionId().length);
    out.writeShort(clientHello.getChallengeData().length);

     /*
      The CIPHER-SPECS-DATA define a cipher type and key length (in bits)
      that the receiving end supports. Each SESSION-CIPHER-SPEC is 3
      bytes long and looks like this:

      char CIPHER-KIND-0
      char CIPHER-KIND-1
      char CIPHER-KIND-2

      Where CIPHER-KIND is one of:

      SSL_CK_RC4_128_WITH_MD5
      SSL_CK_RC4_128_EXPORT40_WITH_MD5
      SSL_CK_RC2_128_CBC_WITH_MD5
      SSL_CK_RC2_128_CBC_EXPORT40_WITH_MD5
      SSL_CK_IDEA_128_CBC_WITH_MD5
      SSL_CK_DES_64_CBC_WITH_MD5
      SSL_CK_DES_192_EDE3_CBC_WITH_MD5

      This list is not exhaustive and may be changed in the future.
     */

    for (SSL2CipherSuite cipherSuite : clientHello.getListSupportedCipherSuites()) {
      out.writeBytes( cipherSuite.getByteValue() );
    }
    out.writeBytes(clientHello.getSessionId());
    out.writeBytes(clientHello.getChallengeData());
  }

  public int bytesNeeded(ClientHello clientHello) {
    return 1  // message type
            + 2  // version
            + 2 // cipherSpecLength
            + 2 // sessionIdLength
            + 2 // challengeLength
            + 3 * clientHello.getListSupportedCipherSuites().size()
            + clientHello.getSessionId().length
            + clientHello.getChallengeData().length;
  }

}
