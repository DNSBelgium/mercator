package be.dnsbelgium.mercator.vat.domain;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class tries to find all (valid) VAT values in a given String.
 * It does NOT know anything about HTTP nor HTML.
 * The class is using Regular Expressions to find Belgian VAT values.
 *
 * Because of the many different ways to format a Belgian VAT number, it's impossible to catch
 * all VAT values and to catch ONLY vat values.
 * The current regex is an attempt to find most real VAT values without catching too many false positives,
 * like phone numbers, bank accounts or other numeric strings.
 *
 */
@Service
public class VatFinder {

  private static final Logger logger = getLogger(VatFinder.class);

  private final Pattern vatPattern;
  public static final String VAT_REGEX =

      "\\b(?:VAT|BTW|TVA)?(?:BE)?:?(?:" +  // always start on a word boundary, optional VAT or BTW or VAT, optional BE, optional colon
          // Possibly followed by a 0 or 1, with three times: three numbers, a possible separator
          "[01]?[ .-]?[0-9]{3}[ .-]?[0-9]{3}[ .-]?[0-9]{3}" +

          // OR:  non-zero digit, optional sep, 2 digits, optional sep, 3 digits, optional sep , 3 digits
          "|[1-9][ .-]?[0-9]{2}[ .-]?[0-9]{3}[ .-]?[0-9]{3}" +

          ")(?![0-9])";  // only matches the above when it is not followed by a digit

  /*
    // TODO add regex for NL and FR ??
    // String NL_VAT_REGEX =  "(?i)((NL)?0([. -])?[0-9]{3}([. -])?[0-9]{3}([. -])?[0-9]{3})B[0-9]{2}";

    https://ondernemersplein.kvk.nl/btw-identificatienummer-opzoeken-controleren-en-vermelden/
    Het btw-identificatienummer (btw-id) bestaat uit de landcode NL, 9 cijfers, de letter B en een controlegetal van 2 cijfers.
    U zet uw btw-id op uw facturen.
    Als u via het internet verkoopt of diensten aanbiedt, moet u het btw-id op uw website zetten
   */

  public VatFinder() {
    this.vatPattern = Pattern.compile(VAT_REGEX, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Normalizes a raw VAT number found on a web page to fit the format BExxxxxxxxxx where x is a digit
   *
   * @author Jules Dejaeghere
   * @param VAT Raw VAT number found on a web page
   * @return Nullable (if input is null) String with the normalized VAT number
   */
  public String normalizeVAT(String VAT) {
    if (VAT == null) {
      return null;
    }
    VAT = VAT.toUpperCase();
    VAT = VAT
        .replace(".", "")
        .replace("-", "")
        .replace(" ", "")
        .replace(":", "")
        .replace("BTW","")
        .replace("VAT", "")
        .replace("TVA", "")
        .replace("BE", "")
    ;
    if (VAT.length() < 10) {
      // Although a VAT can have another leading number, we assume that people omitting it will have a zero as leading
      // number.
      return "BE0" + VAT;
    }
    return "BE" + VAT;
  }

  /**
   * Determines if a normalized VAT number is valid.
   * A valid VAT number meet the following criteria:
   *  - Let BE0xxx xxx xyy a VAT number
   *  - The VAT number is valid if 97-(xxxxxxx mod 97) == yy
   *
   * @param VAT   Normalized VAT number to check
   * @return      true if the VAT is valid, false otherwise
   */
  public boolean isValidVAT(String VAT) {
    if (VAT == null || VAT.length() < 9) {
      return false;
    }
    VAT = VAT.substring(2);
    int head = Integer.parseInt(VAT.substring(0, VAT.length() - 2));
    int tail = Integer.parseInt(VAT.substring(8));
    int checksum = (97 - (head % 97));
    logger.debug("input={} head={} tail={} checksum={}", VAT, head, tail, checksum);
    return (checksum == tail);
  }

  public List<String> findVatValues(String text) {
    List<String> vatList = new ArrayList<>();
    Matcher matcher = vatPattern.matcher(text);
    while (matcher.find()) {
      String match = matcher.group(0);
      String VAT = normalizeVAT(match);
      if (!vatList.contains(VAT)) {
        vatList.add(VAT);
      }
    }
    return vatList;
  }

  public List<String> findValidVatValues(String text) {
    List<String> vatList = findVatValues(text);
    return vatList.stream().filter(this::isValidVAT).collect(Collectors.toList());
  }

}
