package be.dnsbelgium.mercator.feature.extraction;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static be.dnsbelgium.mercator.feature.extraction.MercatorLanguageDetector.LanguageSelection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@Disabled("These tests take over a minute and mostly test 3rd party code")
class MercatorLanguageDetectorTest {

  private final MercatorLanguageDetector languageDetector = new MercatorLanguageDetector();
  private static final Logger logger = getLogger(MercatorLanguageDetectorTest.class);

  List<Language> languages = List.of(
      Language.BULGARIAN, Language.CROATIAN, Language.CZECH, Language.DANISH, Language.DUTCH, Language.ENGLISH,
      Language.ESTONIAN, Language.FINNISH, Language.FRENCH, Language.GERMAN, Language.GREEK, Language.HUNGARIAN,
      Language.IRISH, Language.ITALIAN, Language.LATVIAN, Language.LITHUANIAN, Language.POLISH, Language.PORTUGUESE,
      Language.ROMANIAN, Language.SLOVAK, Language.SLOVENE, Language.SPANISH, Language.SWEDISH, Language.ARABIC,
      Language.PERSIAN, Language.JAPANESE, Language.CHINESE, Language.RUSSIAN
  );

  @Test
  public void nl() {
    String lang = languageDetector.detectLanguageOf("De wereld staat op zijn kop.", COMMON_LANGUAGES);
    logger.info("lang = {}", lang);
    assertThat(lang).isEqualTo("nl");
  }

  @Test
  public void html() {
    String lang = languageDetector.detectCommonLanguageOf("<body><p>Hier staat wat tekst</p></body>");
    logger.info("lang = {}", lang);
    assertThat(lang).isEqualTo("nl");
  }

  @Test
  public void unknown() {
    String iso = Language.UNKNOWN.getIsoCode639_1().toString();
    logger.info("iso = {}", iso);
    assertThat(iso).isEqualTo("none");
  }

  @Test
  public void createInstance() {
    for (int i=0; i<20; i++) {
      LocalDateTime start = LocalDateTime.now();
      MercatorLanguageDetector languageDetector = new MercatorLanguageDetector();
      languageDetector.detectLanguageOf("Een beetje tekst", ALL_SPOKEN_LANGUAGES);
      Duration duration = Duration.between(start, LocalDateTime.now());
      logger.info("Iteration {} : duration = {}", i, duration);
      if (i > 0) {
        // only the first instantation takes a long time
        assertThat(duration.toMillis()).isLessThan(100);
      }
    }
  }

  @Test
  public void memoryUsagePerLanguage() {
    long previousMemoryUsed = usedMemory();
    for (Language language : languages) {
      LocalDateTime start = LocalDateTime.now();
      LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.INDONESIAN, language).build();
      detector.detectLanguageOf("Een beetje tekst");
      long usedMemory = usedMemory();
      long extraMemoryUsed = usedMemory - previousMemoryUsed;
      previousMemoryUsed = usedMemory;
      String millis = StringUtils.leftPad(""+ Duration.between(start, LocalDateTime.now()).toMillis(), 3);
      String lang = StringUtils.rightPad(language.name(), 20);
      logger.info("language = {} = {} => duration = {}ms memory usage: {} MB => Increase: {} MB",
          language.getIsoCode639_1(), lang, millis, usedMemory, extraMemoryUsed);
      System.gc();
    }
    System.gc();
    MercatorLanguageDetector languageDetector = new MercatorLanguageDetector();
    languageDetector.detectLanguageOf("nog wat tekst", ALL_SPOKEN_LANGUAGES);
    logger.info("Memory used = {} Mb", usedMemory());
  }

  private long usedMemory() {
    int mb = 1024 * 1024;
    Runtime instance = Runtime.getRuntime();
    return (instance.totalMemory() - instance.freeMemory()) / mb;
  }

}