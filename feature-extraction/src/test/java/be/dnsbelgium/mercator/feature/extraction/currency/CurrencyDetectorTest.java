package be.dnsbelgium.mercator.feature.extraction.currency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CurrencyDetectorTest {

  private static final CurrencyDetector currencyDetector = new CurrencyDetector();

  @Test
  public void testCurrencies() {
    String text = "Some currencies in the HTML page IRR\n" + "EUR GBP USD USD EUR\n" + "$ € £\n" + "лв 10,25";

    CurrencyDetectorResult result = currencyDetector.detect(text);
    assertThat(result.getNbOccurrences()).isEqualTo(10);
    assertThat(result.getNbDistinct()).isEqualTo(5);
  }

  @Test
  public void testCurrenciesWithText() {
    String text = "The currency names detection should only pick currency names that are separated from the rest, like " +
        "EUR but not EURsomething.\n" + "However, it is fine if the currency is next to a number 1EUR or 12,50€";

    CurrencyDetectorResult result = currencyDetector.detect(text);
    assertThat(result.getNbOccurrences()).isEqualTo(3);
    assertThat(result.getNbDistinct()).isEqualTo(1);
  }
}
