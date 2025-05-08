package be.dnsbelgium.mercator.feature.extraction;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MercatorLanguageDetectorTest {

    private static final Logger logger = LoggerFactory.getLogger(MercatorLanguageDetectorTest.class);
    private static final MercatorLanguageDetector   MODEL = new MercatorLanguageDetector();

    @Test
    public void detectDutch() {
        MercatorLanguageDetector detector = new MercatorLanguageDetector();
        var lang = detector.detectCommonLanguageOf("Ik ben soms stout");
        logger.info("lang = {}", lang);
        assertThat(lang).isEqualTo("nl");
    }

    @Test
    public void detectDutchMemoryUsage() {
        String lang = MODEL.detectCommonLanguageOf("Ik ben soms stout");
        logMemoryUsage();
        logger.info("Detected language: {}", lang);
        assertThat(lang).isEqualTo("nl");
    }

    private void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        logger.info("Memory Usage: Used={}MB, Total={}MB, Max={}MB", usedMemory, totalMemory, maxMemory);
    }

}