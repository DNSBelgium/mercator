package be.dnsbelgium.mercator.tls.domain.ssl2;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CodecException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class TlsDecoder {

  private static final Logger logger = getLogger(TlsDecoder.class);

  public static boolean incompleteData(ByteBuf in) {
    if (in.readableBytes() < 3) {
      logger.debug("Not enough data available to read tlsRecordLength. readableBytes: {}", in.readableBytes());
      return true;
    }
    short[] headerData = new short[3];
    headerData[0] = in.getUnsignedByte(in.readerIndex());
    headerData[1] = in.getUnsignedByte(in.readerIndex() + 1);
    headerData[2] = in.getUnsignedByte(in.readerIndex() + 2);
    TlsRecordHeader header = decodeHeader(headerData);
    logger.debug("header = {}", header);

    if (in.readableBytes() < header.recordLength + 2) {
      logger.debug("Not enough data available to read complete TLS record. header.recordLength={}, readableBytes={}",
          header.recordLength, in.readableBytes());
      return true;
    }
    logger.debug("We have enough data to read complete TLS record. tlsRecordLength: {}", header.recordLength);
    // Now we can remove the header bytes from the buffer
    for (int i=0; i < header.headerLength; i++) {
      in.readByte();
    }
    return false;
  }

  /**
   * Decode the input according to <a href="https://datatracker.ietf.org/doc/html/draft-hickman-netscape-ssl-00">draft-hickman-netscape-ssl-00</a>
   *
   *     If the most significant bit is set in the first byte of the record length code
   *     then the record has no padding and the total header length will be 2 bytes,
   *     otherwise the record has padding and the total header length will be 3 bytes.
   *     The record header is transmitted before the data portion of the record.
   *
   *     Note that in the long header case (3 bytes total), the second most
   *     significant bit in the first byte has special meaning. When zero, the record
   *     being sent is a data record. When one, the record being sent is a security
   *     escape (there are currently no examples of security escapes; this is
   *     reserved for future versions of the protocol). In either case, the length code
   *     describes how much data is in the record.
   *
   *     When the 3 byte header is used, the record length is computed as follows (using a "C"-like notation):
   *
   *     RECORD-LENGTH = ((byte[0] & 0x3f) << 8)) | byte[1];
   *     IS-ESCAPE = (byte[0] & 0x40) != 0;
   *     PADDING = byte[2];
   *
   *     The record header defines a value called PADDING. The PADDING
   *     value specifies how many bytes of data were appended to the original
   *     record by the sender. The padding data is used to make the record length
   *     be a multiple of the block ciphers block size when a block cipher is used
   *     for encryption.
   *
   * @param bytes an array of either two or 3 bytes
   * @return a corresponding TlsRecordHeader object
   */
  public static TlsRecordHeader decodeHeader(short[] bytes) {
    for (short aByte : bytes) {
      if (aByte < 0) {
        throw new IllegalStateException("decodeHeader only supports unsigned bytes, but input was [" + hexString(bytes) + "]");
      }
    }
    // check most significant bit of first byte to see if we need to interpret two bytes or three bytes
    int headerLength = (bytes[0] & 0x80) != 0 ? 2 : 3;
    if (headerLength == 2) {
      short recordLength = (short) (((bytes[0] & 0x7f) << 8) | bytes[1]);
      return new TlsRecordHeader(recordLength);
    }
    short recordLength = (short) (((bytes[0] & 0x3f) << 8) | bytes[1]);
    boolean securityEscape = (bytes[0] & 0x40) != 0;
    int padding = bytes[2];
    return new TlsRecordHeader(recordLength, securityEscape, padding);
  }

  public static short[] encode(TlsRecordHeader recordHeader) {
    if (recordHeader.padding > 0 || recordHeader.securityEscape) {
      if (recordHeader.headerLength != 3) {
        throw new CodecException("header must be 3 bytes when there is padding");
      }
      short[] bytes = new short[3];
      bytes[0] = (short) (recordHeader.recordLength / 256);
      bytes[1] = (short) (recordHeader.recordLength - (bytes[0] * 256));
      bytes[2] = recordHeader.padding;
      if (recordHeader.securityEscape) {
        // the second most significant bit in the first byte has special meaning
        bytes[0] = (short) (bytes[0] | 64);
      }
      return bytes;
    }
    if (recordHeader.headerLength != 2) {
      throw new CodecException("header must be 2 bytes when there is no padding");
    }
    short byte0 = (short) (recordHeader.recordLength / 256);
    short byte1 = (short) (recordHeader.recordLength - (byte0 * 256));
    // Now switch on the most significant bit of byte1
    byte0 = (short) (byte0 + 128);
    return new short[] { byte0, byte1};
  }

  public static void write(TlsRecordHeader recordHeader, ByteBuf out) {
    short[] headerBytes = TlsDecoder.encode(recordHeader);
    for (int headerByte : headerBytes) {
      out.writeByte(headerByte);
    }
  }

  private static String hexString(short[] values) {
    byte[] bytes = new byte[values.length];
    for (int i=0; i<values.length; i++) {
      bytes[i] = (byte) values[i];
    }
    return Hex.encodeHexString(bytes).toUpperCase();
  }

}
