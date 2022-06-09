package be.dnsbelgium.mercator.tls.domain.ssl2;

import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.ssl2.SSL2CipherSuite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class ServerHelloTest {

  private static final Logger logger = getLogger(ServerHelloTest.class);
  ServerHelloEncoder encoder = new ServerHelloEncoder();
  ServerHelloDecoder decoder = new ServerHelloDecoder();

  @Test
  public void encodeDecode() {
    byte[] connectionId = new byte[0];
    ServerHello serverHello = new ServerHello(
        false,
        1,
        new byte[] { 0x01, 0x02 },
        new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10 },
        List.of(SSL_CK_DES_64_CBC_WITH_MD5),
        connectionId
    );
    logger.info("serverHello = {}", serverHello);
    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(100);
    logger.info("buf = {}", buf);
    encoder.encode(null, serverHello, buf);
    logger.info("buf = {}", buf);

    List<Object> decodedMessages = new ArrayList<>();
    decoder.decode(null, buf, decodedMessages);
    logger.info("decodedMessages = {}", decodedMessages.size());
    for (Object decodedMessage : decodedMessages) {
      logger.info("decodedMessage = {}", decodedMessage);
    }
    assertThat(decodedMessages.size()).isEqualTo(1);
    ServerHello decoded = (ServerHello) decodedMessages.get(0);
    assertThat(decoded).isEqualTo(serverHello);
  }


  @Test
  public void encodeWithConnectionId() {
    byte[] connectionId = new byte[] { 1, 2, 3, 4, 5};
    ServerHello serverHello = new ServerHello(
        true,
        1,
        new byte[] { 0x00, 0x02 },
        new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10 },
        List.of(SSL_CK_DES_64_CBC_WITH_MD5, SSL_CK_IDEA_128_CBC_WITH_MD5),
        connectionId
    );
    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(100);
    encoder.encode(null, serverHello, buf);
    List<Object> decodedMessages = new ArrayList<>();
    decoder.decode(null, buf, decodedMessages);
    assertThat(decodedMessages.size()).isEqualTo(1);
    ServerHello decoded = (ServerHello) decodedMessages.get(0);
    assertThat(decoded).isEqualTo(serverHello);
  }

  @Test
  public void decode() throws DecoderException {
    // captured with Wireshark
    String hex = "88500400010002082f000600103082082b30820713a003020102021203022084e7dd994fbcf74320249d73493132300d06092a864886f70d01010b05003032310b300906035504061302555331163014060355040a130d4c6574277320456e6372797074310b3009060355040313025233301e170d3232303131373037313133325a170d3232303431373037313133315a301f311d301b0603550403131462726f6f647761792d6d6561746578706f2e6265308201a2300d06092a864886f70d01010105000382018f003082018a02820181008b7b44d58655b4d86eacc6f5eb7e7b2c7565e0e7d78ef1c42e42a97e981714aaa88427646e629b3815c56151f73405fc3067b68deb67d86831b880a84f79b774a97a8c9fcba569ab49f544e6b155fdf0c8e39ffded307960c53e7ae31ebe599b0285925aee7890c53643f2e208c3da0008d925ba02e3ab5f5e76469a2a1eaff2ab8fbf1be1fd485858bb3922e58056f7d6b2ac8174ca51a71837c15cdc6cf2caacc350c91fd7461ebb3abd863c92546b0cd0f87c721c46a9e28cd74c98ddecd99956f17300bc1f9b0d1ec1f973d933a3a17b5e4129874fd2d2d8f397d51a32944e18a1d689b2bf82a0d9c6161304179248e7e4fda7632ccfa64b465ddeab4d183fb2c98751b09a01aec34f158ca39b8e54c7b606852e809fa366a16fca6aeab5b91ad898c0160f14b3afdc5010bd8cbae7474e3e34087ea94b314bffd52ccccb5f0c169585dc4283d6ad45b79cbcf032f7cfa3a75723b69f22cf626f908cabd3e61998339f9ea51982a5347765ee323ca0e7c81da45b0196fff7703a3dec682b0203010001a38204cc308204c8300e0603551d0f0101ff0404030205a0301d0603551d250416301406082b0601050507030106082b06010505070302300c0603551d130101ff04023000301d0603551d0e041604145e224a5b7d6d0510dc388f9bf2d00c7301662922301f0603551d23041830168014142eb317b75856cbae500940e61faf9d8b14c2c6305506082b0601050507010104493047302106082b060105050730018615687474703a2f2f72332e6f2e6c656e63722e6f7267302206082b060105050730028616687474703a2f2f72332e692e6c656e63722e6f72672f3082029b0603551d11048202923082028e821462726f6f647761792d6d6561746578706f2e6265820a6368656678706f2e6265820e6575726f646f6773686f772e626582166575726f646f6773686f772e6c6971756966692e6265820a696e666f706f6c2e6265820a696e666f706f6c2e65758212696e666f706f6c2e6c6971756966692e62658214696e746572706f6d2d7072696d657572732e6265820b6a757374626565722e626582196b6f727472696a6b2e652d636f6d6d6572636578706f2e626582186b6f727472696a6b2e65636f6d6d6572636578706f2e626582166b6f727472696a6b78706f2e6c6971756966692e626582166c696567652e652d636f6d6d6572636578706f2e626582156c696567652e65636f6d6d6572636578706f2e626582126e6f72646261742e6c6971756966692e6265821670726f746f747970696e672e6c6971756966692e6265821270726f746f747970696e6778706f2e636f6d820d7461766f6c612d78706f2e626582117468656861697270726f6a6563742e657582107574696c697479326275696c642e626582187777772e62726f6f647761792d6d6561746578706f2e6265820e7777772e6368656678706f2e626582127777772e6575726f646f6773686f772e6265820e7777772e696e666f706f6c2e6265820e7777772e696e666f706f6c2e657582187777772e696e746572706f6d2d7072696d657572732e6265820f7777772e6a757374626565722e626582167777772e70726f746f747970696e6778706f2e636f6d82117777772e7461766f6c612d78706f2e626582157777772e7468656861697270726f6a6563742e657582147777772e7574696c697479326275696c642e626582127777772e78706f6361746572696e672e6265820e78706f6361746572696e672e6265304c0603551d20044530433008060667810c0102013037060b2b0601040182df130101013028302606082b06010505070201161a687474703a2f2f6370732e6c657473656e63727970742e6f726730820103060a2b06010401d6790204020481f40481f100ef0075002979bef09e393921f056739f63a577e5be577d9c600af8f94d5d265c255dc7840000017e671760c70000040300463044022009327da3b68b45fc28a4460d8af54297c3eba87cfcbc684f2728a40d1fadf18c02203a21b9a2f8b37f6943528dc2d0a6451ed9733d41ad8f4218d1080186c405e11c00760041c8cab1df22464a10c6a13a0942875e4e318b1b03ebeb4bc768f090629606f60000017e671762b8000004030047304502205ceb337ce58d41767623a86e38327b6ef3021a33d64d591c2e854ee708771a87022100f55dab0333f154f0f3efb63c364e9bdce10ed6e127cd7dfa6a7c9fdda763dcc8300d06092a864886f70d01010b050003820101009b8f9226778f6ea7529595359426370ba25c9a0daf920a66558c95840d109458fd997e95093006cb66d4c915570e900638b971a7ec275c97bbd7e889742e59b0dd92d0bed83661d83cbfb8a3704669ffaa389b77153961c7cd05ac491157080e3f3277cdd6749bb418157321f447399834eb2c4dec9c9ec8b6d7c68c84a28802736f0ca0ff895c8a297efa33288251bb5e272f45ffa73714ebf22401d6f410e9b9e8f6862fb7381c8472d55fd3bc0858fd42899fe49feca8929f18aa9bffd6ead76577e893b015d43a9f9f2e4ff0c8af5c4a717fb0ee3c8b9fafc910846d9480444a0612d74584fd04e9912499322bacca849abd0d8def699ce2cc38ab4d13620100800700c094fc3ece0e797142449a52e4a39aad92";
    byte[] data = Hex.decodeHex(hex);

    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(data.length);
    buf.writeBytes(data);

    List<Object> decodedMessages = new ArrayList<>();
    decoder.decode(null, buf, decodedMessages);
    logger.info("decodedMessages = {}", decodedMessages);
    assertThat(decodedMessages).hasSize(1);
    ServerHello serverHello = (ServerHello) decodedMessages.get(0);
    assertThat(serverHello.getConnectionId()).hasSize(16);
    assertThat(serverHello.getCertificate()).hasSize(2095);
    assertThat(serverHello.selectedVersion()).isEqualTo(TlsProtocolVersion.SSL_2.getName());
    assertThat(serverHello.getListSupportedCipherSuites()).containsExactly(
        SSL_CK_RC4_128_WITH_MD5, SSL_CK_DES_192_EDE3_CBC_WITH_MD5);
  }

}
