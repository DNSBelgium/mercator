package be.dnsbelgium.mercator.vat.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class VatFinderTest {

  private final VatFinder vatFinder = new VatFinder();
  private static final Logger logger = getLogger(VatFinderTest.class);

  @Test
  public void testNormalizeVAT() {
    String[] raw = {
        "BE0123 456 346",
        "BE0123-456-346",
        "BE0123.456.346",
        "0123 456 346",
        "0123456346",
        "0123.456.346",
        "123.456.346",
        "BE 123.456.346",
        "123 456 346"
    };
    String normalized;
    for (String vat : raw) {
      normalized = vatFinder.normalizeVAT(vat);
      assertThat(normalized).isEqualTo("BE0123456346");
    }
  }

  @Test
  public void testIsValidVAT() {
    String[] valid = {"BE0666679317", "BE0457741515", "BE0843370953", "BE1000007697"};
    for (String vat : valid) {
      Assertions.assertTrue(vatFinder.isValidVAT(vat));
    }
  }

  @Test
  public void testInvalidVAT() {
    // invalid checksums
    String[] invalid = {"BE0666679300", "BE0457741542", "BE0843370973", "BE1000009797"};
    for (String vat : invalid) {
      Assertions.assertFalse(vatFinder.isValidVAT(vat));
    }
  }

  @Test
  public void findMultipleVatValues() {
    String input = "abc BE0666679317 cdef BE0457741515 xyz BE0843370953";
    List<String> list = vatFinder.findVatValues(input);
    logger.info("list = {}", list);
    assertThat(list).containsExactly("BE0666679317", "BE0457741515", "BE0843370953");
  }

  @Test
  public void noDuplicatesInResult() {
    String input = "abc BE0666679317 cdef BE0457741515 xyz BE0666679317";
    List<String> list = vatFinder.findVatValues(input);
    logger.info("list = {}", list);
    assertThat(list).containsExactly("BE0666679317", "BE0457741515");
  }

  @Test
  public void VatNumbersCanStartWithANonZeroNumber() {
    String input = "BE1000000000 BTW BE 1100000002 1000.000.003";
    List<String> list = vatFinder.findVatValues(input);
    logger.info("list = {}", list);
    assertThat(list).containsExactly("BE1000000000", "BE1100000002", "BE1000000003");
  }

  @Test
  public void phoneNumbers() {
    String text =
        "  le numéro d’entreprise est le 0416.971.722 ;\n" +
        "  le numéro de compte bancaire est le BE07 0682 0302 4966\n" +
        "  le numéro d’entreprise est le 0416.971.722   \n" +
        "  the pone number            is 0497.489 739  \n";

    expect(text, List.of("BE0416971722", "BE0497489739"), List.of("BE0416971722", "BE0497489739"));

    // Mobile phone numbers look like KBO numbers and sometimes the checksum is correct
    // TODO: we could check which mobile prefixes never occur in the KBO database and remove them from the results
    // For example: there are no KBO numbers that start with 049 so we can discard those
    // We could go one step further and only keep VAT numbers when they are present in the KBO db

    expectNoVatValuesIn("Tel. 0488/516645");
    expect("+32 (0) 488516645", List.of("BE0488516645"), List.of("BE0488516645"));
    String text2 = "N° d’entreprise :      0462.952.393.\nN° de TVA :     BE0462.952.393.";
    expect(text2, List.of("BE0462952393"), List.of("BE0462952393"));
  }

  @Test
  public void wonderland() {
    // Our first two crawls did not detect a VAT value although the body text contains "BE 638.623.155"
    String bodyText = "Boverststraat 21b 4321 Wonderland BE 638.623.155 069/251001 069/251001 0499/22193 0499/22193 ? ALICE";
    logger.info("bodyText = [{}]", bodyText);
    List<String> all = vatFinder.findVatValues(bodyText);
    logger.info("all = {}", all);
    assertThat(all).containsExactly("BE0638623155", "BE0251001069");
    List<String> valid = vatFinder.findValidVatValues(bodyText);
    logger.info("valid = {}", valid);
    assertThat(valid).containsExactly("BE0638623155");
  }

  @Test
  public void phoneNumbersAndVatNumbers() {
    String text1 = "Blablabla: Donald duck BVBA (BE0828146606)\n" +
        "•    Goofy (FSMA 13180 A-cB, BE0430267848)\n" +
        "•    Mickey Mouse (FSMA 112654 A, BE0542999070)\n" +
        "•    Pluto (FSMA 22879 A-cB, BE0437928769)\n" +
        "•    Captain Hook (BIV 203698)";
    expect(
        text1,
        List.of("BE0828146606", "BE0430267848", "BE0542999070", "BE0437928769"),
        List.of("BE0828146606", "BE0430267848", "BE0542999070", "BE0437928769")
    );
    expect(
        "Telefoon: +32 476 559 515 BE 0664.641.129 Email:",
        List.of("BE0476559515", "BE0664641129"),
        List.of("BE0476559515", "BE0664641129")
    );

    String text3 = "Big island 3 1234 Neverland T 011 22 33 44 M 0499 102 467 F 011 11 33 55 BE 714 795 968 Blabla " +
        "Small island 27 1234 Neverland BE 0770 017 276 Blop";

    expect(text3, List.of("BE0499102467", "BE0714795968", "BE0770017276"), List.of("BE0714795968", "BE0770017276"));

    // TODO: +32 999 999 999 is considered as a VAT value (with a wrong checksum)
    expect("+32 999 999 999", List.of("BE0999999999"), List.of());

    // if the last 9 digits of a phone number happen to have correct check digits, they are mis-recognized as a VAT number
    // but only if the character directly before the 9 digits is 0 or a non-digit
    expect("Peter Pan 0032 (0)476 305 236 peter.pan@notdnsbelgium.be",
        List.of("BE0476305236"),
        List.of("BE0476305236")
    );
    expect("tel 0498 577 487 - captain.hooknfo@notdnsbelgium.be - BE0862.100.762",
        List.of("BE0498577487","BE0862100762"),
        List.of("BE0862100762")
    );
  }

  @Test
  public void goodInputs() {
    String[] inputs = new String[] {
      "BE 0123456789",
      "BE0-123-456-789",
      "BE0123456789",
      "0123456789",
      "0.123456789",
      "0-123-456-789",
      "BE 123-456-789",
      "123-456-789",
      "BTW:BE0123456789",
      "123456789"
    };
    String expected = "BE0123456789";
    for (String input : inputs) {
      expect(input, List.of(expected), List.of());
    }
  }

  @Test
  public void badInputs() {
    String[] bad_inputs = new String[] {
        "01234567890",     // too many digits
        "0-1234567890",    // too many digits
        "12345678901",     // too many digits
        "0479/23.45.78",   // slash not allowed as separator
        "016 123456 0479/23.45.78"
    };
    //expectNoVatValuesIn("0-1234567890");
    expectNoVatValuesIn("12345678901");
    expectNoVatValuesIn("0479/23.45.78");
    expectNoVatValuesIn("016 123456 0479/23.45.78");
    // unfortunately these strings are still recognized as a VAT number:
    expect("0-123-456-789-0123", List.of("BE0123456789"), List.of());
    expect("0-123-456-789-1",    List.of("BE0123456789"), List.of());
  }

  public void expectNoVatValuesIn(String input) {
    expect(input, List.of(), List.of());
  }

  public void expect(String input, List<String> expectedVatValues, List<String> expectedValidVatValues) {
    List<String> actualMatches = vatFinder.findVatValues(input);
    if (actualMatches.equals(expectedVatValues)) {
      logger.info("OK: input=[{}] => matches = {}", input, actualMatches);
    } else {
      logger.warn("NOT OK: input=[{}] => expected {} but found {}", input, expectedVatValues, actualMatches);
    }
    assertThat(actualMatches).isEqualTo(expectedVatValues);

    List<String> actualValidMatches = vatFinder.findValidVatValues(input);
    if (actualValidMatches.equals(expectedValidVatValues)) {
      logger.info("OK: input=[{}] => valid matches = {}", input, expectedValidVatValues);
    } else {
      logger.warn("NOT OK: input=[{}] => expected valid matches: {} but found {}", input, expectedValidVatValues, actualValidMatches);
    }
    assertThat(actualValidMatches).isEqualTo(expectedValidVatValues);
  }

}