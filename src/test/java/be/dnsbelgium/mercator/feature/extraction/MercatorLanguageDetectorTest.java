package be.dnsbelgium.mercator.feature.extraction;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled // TODO solve the OOM
class MercatorLanguageDetectorTest {

    @Test
    public void detectDutch() {
        MercatorLanguageDetector detector = new MercatorLanguageDetector();
        var lang = detector.detectCommonLanguageOf("Ik ben soms stout");
        System.out.println("lang = " + lang);
    }

}