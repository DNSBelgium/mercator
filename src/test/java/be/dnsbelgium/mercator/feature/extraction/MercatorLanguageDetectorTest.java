package be.dnsbelgium.mercator.feature.extraction;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled // TODO solve the OOM
class MercatorLanguageDetectorTest {

    private static final Logger logger = LoggerFactory.getLogger(MercatorLanguageDetectorTest.class);

    @Test
    public void detectDutch() {
        MercatorLanguageDetector detector = new MercatorLanguageDetector();
        var lang = detector.detectCommonLanguageOf("Ik ben soms stout");
        logger.info("lang = {}", lang);
    }

}