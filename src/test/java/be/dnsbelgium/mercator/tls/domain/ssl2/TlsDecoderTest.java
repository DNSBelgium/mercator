package be.dnsbelgium.mercator.tls.domain.ssl2;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class TlsDecoderTest {

  private static final Logger logger = getLogger(TlsDecoderTest.class);

  @Test
  public void encodeTlsRecordHeader() {
    for (int i=0; i<=32767; i++) {
      TlsRecordHeader header = new TlsRecordHeader(i);
      short[] bytes = TlsDecoder.encode(header);
      //logger.info("i={} => {}", i, bytes);
      TlsRecordHeader header2 = TlsDecoder.decodeHeader(bytes);
      assertThat(header2).isEqualTo(header);
    }
  }

  // #define SSL_MAX_RECORD_LENGTH_2_BYTE_HEADER	32767
  // #define SSL_MAX_RECORD_LENGTH_3_BYTE_HEADER	16383


  @Test
  public void encodeTlsRecordHeaderWithPadding() {
    int headersEncoded = 0;
    for (int i=1; i<=16383; i++) {
      for (int padding=1; padding<255; padding++) {
        for (boolean escape : List.of(true, false)) {
          TlsRecordHeader header = new TlsRecordHeader(i, escape, (short) padding);
          short[] bytes = TlsDecoder.encode(header);
          TlsRecordHeader decoded = TlsDecoder.decodeHeader(bytes);
          assertThat(decoded).isEqualTo(header);
          headersEncoded++;
        }
      }
    }
    logger.info("headersEncoded = {}", headersEncoded);
  }

  @Test
  public void header() {
    short[] headerData = new short[] { 0x88, 0x50, 0x10 };
    TlsRecordHeader header = TlsDecoder.decodeHeader(headerData);
    logger.info("header = {}", header);
    TlsRecordHeader expected = new TlsRecordHeader(2128);
    assertThat(header).isEqualTo(expected);
  }

}
