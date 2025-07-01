package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.test.ResourceReader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlFeatureExtractorTest {

    MeterRegistry registry = new SimpleMeterRegistry();
    private static final Logger logger = LoggerFactory.getLogger(HtmlFeatureExtractorTest.class);

    @SneakyThrows
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @Disabled(value= "This test takes around 10 seconds...")
    public void extractFromHtml() {
        HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(registry, false);
        String url = "https://www.yana-gifts.be/algemene-voorwaarden";
        String html = ResourceReader.readFileToString("contents/big.html");
        logger.info("html has length of {}", html.length());
        // this used to take 10 minutes ...
        HtmlFeatures htmlFeatures = featureExtractor.extractFromHtml(html, url, "yana-gifts.be");
        logger.info("htmlFeatures = {}", htmlFeatures);
        logger.info("htmlFeatures.nb_numerical_strings = {}", htmlFeatures.nb_numerical_strings);
        logger.info("htmlFeatures.nb_links_int = {}", htmlFeatures.nb_links_int);

        assertThat(htmlFeatures.nb_links_int).isEqualTo(41);

    }

}