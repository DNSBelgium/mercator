package be.dnsbelgium.mercator.feature.extraction;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class TagMapperTest {

  private final TagMapper tagMapper = new TagMapper();
  private static final Logger logger = getLogger(TagMapperTest.class);
  private static final String URL = "http://www.example.com";

  @Test
  public void emptyDocument() {
    String compressed = tagMapper.compress(Document.createShell(URL));
    logger.info("compressed = {}", compressed);
    assertThat(compressed).isEqualTo("VSm");
  }

  @Test
  public void simpleDocument() {
    Document doc = Jsoup.parse(ResourceReader.readFileToString("classpath:/html/simple.html"));
    String compressed = tagMapper.compress(doc);
    logger.info("compressed = {}", compressed);
    assertThat(compressed).isEqualTo("VS99óm+");
  }

  @Test
  public void dnsbelgium() {
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    Document document = Jsoup.parse(html, "https://www.dnsbelgium.be/nl");
    String compressed = tagMapper.compress(document);
    logger.info("compressed = {}", compressed);
    assertThat(compressed).isEqualTo("VS55559999555@@@ó9999999995555m%XCCCCCTCa_ë¿``o______C#ù4a4a4a4aaë¿_#Cù4a4a4a4a4a4aCo__ë¿ù4a4a4a6CCCCMCC¿CCJYCù4CaCOaCCa4CaCOaC+CaCù4CaCOa_CCa4CaCOa_CCa4CaCOa_CCa4CaCOa_CCa4CaCOa_CCa4CaC_Oa_CCaCù4CaCOaCCa4CaCOaC+Ca4CaCOaC+CaCCCCNCC_ë¿_C_ë¿_C_ë¿_CCCCCNCCKC#ù4aù4a4a4a4a4aù4a4a4a4a4a4aù4a4a4a4a4a4a4aù4a4a4a4a4a4aù4a4a4a4a4a4aù4a4a4aù4a4a4a4a4a4a4a4ù4aY4aY4aYCù4a_ë¿}4a_ë¿}4a4a44Caë¿_@@@@@@@@@@@");
  }

}