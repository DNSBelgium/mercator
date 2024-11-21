package be.dnsbelgium.mercator.feature.extraction;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;

public class MercatorLanguageDetector {

  private final LanguageDetector languageDetectorCommon = LanguageDetectorBuilder.fromLanguages(
      Language.BULGARIAN, Language.CROATIAN, Language.CZECH, Language.DANISH, Language.DUTCH, Language.ENGLISH,
      Language.ESTONIAN, Language.FINNISH, Language.FRENCH, Language.GERMAN, Language.GREEK, Language.HUNGARIAN,
      Language.IRISH, Language.ITALIAN, Language.LATVIAN, Language.LITHUANIAN, Language.POLISH, Language.PORTUGUESE,
      Language.ROMANIAN, Language.SLOVAK, Language.SLOVENE, Language.SPANISH, Language.SWEDISH, Language.ARABIC,
      Language.PERSIAN, Language.JAPANESE, Language.CHINESE
  ).build();

  private final LanguageDetector languageDetectorAll = LanguageDetectorBuilder.fromAllSpokenLanguages().build();

  public enum LanguageSelection { ALL_SPOKEN_LANGUAGES, COMMON_LANGUAGES }

  /**
   * Tries to detect the language of the given text
   * @param text the text of which you want to detect the language
   * @param languageSelection the set of languages to use
   * @return the ISO 2-letter code of the detected language or null if no language was detected
   */
  public String detectLanguageOf(String text, LanguageSelection languageSelection) {
    Language language;
    if (languageSelection == LanguageSelection.COMMON_LANGUAGES) {
      language = languageDetectorCommon.detectLanguageOf(text);
    } else {
      language = languageDetectorAll.detectLanguageOf(text);
    }
    if (language == Language.UNKNOWN) {
      return null;
    }
    return language.getIsoCode639_1().toString();
  }

  public String detectCommonLanguageOf(String text) {
    return detectLanguageOf(text, LanguageSelection.COMMON_LANGUAGES);
  }

}
