package be.dnsbelgium.mercator.feature.extraction.currency;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public class CurrencyDetector {

  private static final Logger logger = getLogger(CurrencyDetector.class);

  // Matches ISO 4217 currency code separated from other text by spacing character or number
  private final Pattern currencyCodePattern = Pattern.compile("(\\b|[0-9])(?<currency>(AED)|(AFN)|(ALL)|(AMD)|(ANG)|" +
      "(AOA)|(ARS)|(AUD)|(AWG)|(AZN)|(BAM)|(BBD)|(BDT)|(BGN)|(BHD)|(BIF)|(BMD)|(BND)|(BOB)|(BRL)|(BSD)|(BTN)|(BWP)|" +
      "(BYN)|(BZD)|(CAD)|(CDF)|(CHF)|(CLP)|(CNY)|(COP)|(CRC)|(CUC)|(CUP)|(CVE)|(CZK)|(DJF)|(DKK)|(DOP)|(DZD)|(EGP)|" +
      "(ERN)|(ETB)|(EUR)|(FJD)|(FKP)|(GBP)|(GEL)|(GGP)|(GHS)|(GIP)|(GMD)|(GNF)|(GTQ)|(GYD)|(HKD)|(HNL)|(HRK)|(HTG)|" +
      "(HUF)|(IDR)|(ILS)|(IMP)|(INR)|(IQD)|(IRR)|(ISK)|(JEP)|(JMD)|(JOD)|(JPY)|(KES)|(KGS)|(KHR)|(KMF)|(KPW)|(KRW)|" +
      "(KWD)|(KYD)|(KZT)|(LAK)|(LBP)|(LKR)|(LRD)|(LSL)|(LYD)|(MAD)|(MDL)|(MGA)|(MKD)|(MMK)|(MNT)|(MOP)|(MRU)|(MUR)|" +
      "(MVR)|(MWK)|(MXN)|(MYR)|(MZN)|(NAD)|(NGN)|(NIO)|(NOK)|(NPR)|(NZD)|(OMR)|(PAB)|(PEN)|(PGK)|(PHP)|(PKR)|(PLN)|" +
      "(PYG)|(QAR)|(RON)|(RSD)|(RUB)|(RWF)|(SAR)|(SBD)|(SCR)|(SDG)|(SEK)|(SGD)|(SHP)|(SLL)|(SOS)|(SPL)|(SRD)|(STN)|" +
      "(SVC)|(SYP)|(SZL)|(THB)|(TJS)|(TMT)|(TND)|(TOP)|(TRY)|(TTD)|(TVD)|(TWD)|(TZS)|(UAH)|(UGX)|(USD)|(UYU)|(UZS)|" +
      "(VEF)|(VND)|(VUV)|(WST)|(XAF)|(XCD)|(XDR)|(XOF)|(XPF)|(YER)|(ZAR)|(ZMW)|(ZWD))(\\b|[0-9])");

  // Same as above but with currencies symbols, from https://www.xe.com/symbols.php
  private final Pattern currencySymbolPattern = Pattern.compile("(؋|\\$|лв|៛|¥|₡|₱|Kč|£|€|¢|﷼|₪|₩|₭|ден" +
      "|₨|₮|₦|₽|Дин|฿|₴|₫|₹)");

  // Map symbols to ISO currency code to be able to count the symbol and the code as only one currency
  private final Map<String, String> currenciesSymbolsMap = Map.ofEntries(
      Map.entry("؋", "AFN"),
      Map.entry("$", "USD"), // Other variant may exist for other countries
      Map.entry("лв", "BGN"),
      Map.entry("៛", "KHR"),
      Map.entry("¥", "JPY"),
      Map.entry("₡", "CRC"), // May also refer to SVC
      Map.entry("₱", "PHP"),
      Map.entry("Kč", "CZK"),
      Map.entry("£", "GBP"),
      Map.entry("€", "EUR"),
      Map.entry("¢", "GHS"),
      Map.entry("﷼", "IRR"), // May also refer to OMR, QAR, SAR, YER.  It is a generic symbol for Rial
      Map.entry("₪", "ILS"),
      Map.entry("₩", "KRW"), // May also refer to KPW
      Map.entry("₭", "LAK"),
      Map.entry("ден", "MKD"),
      Map.entry("₹", "INR"),
      Map.entry("₨", "NPR"), // May also refer to MUR, PKR, SCR, LKR.  It is a generic symbol for Rupee
      Map.entry("₮", "MNT"),
      Map.entry("₦", "NGN"),
      Map.entry("₽", "RUB"),
      Map.entry("Дин", "RSD"),
      Map.entry("฿", "THB"),
      Map.entry("₴", "UAH"),
      Map.entry("₫", "VND")
  );

  /**
   * Detect currency names in the given text.
   * The number of time a currency is found and the number of unique currency names are set in the HtmlFeature object
   * This uses the regex and the map defined at the beginning of the class to match currency names and symbols.  When a
   * currency symbol is found, it is looked up in the map to avoid double count of the same currency: e.g. € and EUR are
   * the same.
   *
   * @param text Jsoup document to search currencies in
   * @return CurrencyDetectorResult representing the result of the detection
   */
  public CurrencyDetectorResult detect(String text) {
    HashSet<String> currencies;
    currencies = new HashSet<>();

    Matcher currencyMatcher, symbolMatcher;
    int nbCodes, nbSymbols;

    currencyMatcher = currencyCodePattern.matcher(text);
    symbolMatcher = currencySymbolPattern.matcher(text);

    nbSymbols = 0;
    nbCodes = 0;

    // Find the currency codes
    while (currencyMatcher.find()) {
      nbCodes++;
      currencies.add(currencyMatcher.group("currency"));
    }

    // Find the currency symbols and map them to their currency codes
    while (symbolMatcher.find()) {
      nbSymbols++;
      currencies.add(currenciesSymbolsMap.get(symbolMatcher.group(0)));
    }

    logger.debug("Found a total of {} currency names ({} distinct)", nbCodes + nbSymbols, currencies.size());
    logger.debug(currencies.toString());


    return new CurrencyDetectorResult(nbCodes + nbSymbols, currencies.size(), currencies);
  }
}
