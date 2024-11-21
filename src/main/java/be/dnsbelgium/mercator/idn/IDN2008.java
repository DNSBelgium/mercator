package be.dnsbelgium.mercator.idn;

import com.ibm.icu.text.IDNA;

import static com.ibm.icu.text.IDNA.NONTRANSITIONAL_TO_ASCII;
import static com.ibm.icu.text.IDNA.NONTRANSITIONAL_TO_UNICODE;

public class IDN2008 {

  public final static int OPTIONS = NONTRANSITIONAL_TO_ASCII | NONTRANSITIONAL_TO_UNICODE;
  public final static IDNA IDNA_INSTANCE = com.ibm.icu.text.IDNA.getUTS46Instance(OPTIONS);

  public static String toASCII(String input) {
    StringBuilder destination = new StringBuilder();
    IDNA.Info info = new IDNA.Info();
    var ascii = IDNA_INSTANCE.nameToASCII(input, destination, info);
    if (info.hasErrors()) {
      var msg = String.format("Could not convert [%s] to ASCII: %s", input, info.getErrors());
      throw new IdnException(msg);
    }
    return ascii.toString();
  }

  public static String toUnicode(String input) {
    StringBuilder destination = new StringBuilder();
    IDNA.Info info = new IDNA.Info();
    var unicode = IDNA_INSTANCE.nameToUnicode(input, destination, info);
    if (info.hasErrors()) {
      var msg = String.format("Could not convert [%s] to UNICODE: %s", input, info.getErrors());
      throw new IdnException(msg);
    }
    return unicode.toString();
  }

}
