package be.dnsbelgium.mercator.smtp.domain.crawler.config;


import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest(classes = TestContext.class , properties = {"smtp.crawler.read-time-out=5s"})
@ActiveProfiles("test")
public class SmtpConfigTest {

    @Autowired SmtpConfig config;

    private static final Logger logger = getLogger(SmtpConfigTest.class);

    @Test
    public void propertiesLoaded() {
        logger.info("config = {}", config);
        // depends on having these values also in test/resources/application-test.properties
        assertThat(config.getEhloDomain()).isEqualTo("test.ehlo.be");
        assertThat(config.getNumThreads()).isEqualTo(3);
        assertThat(config.getInitialResponseTimeOut()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.getReadTimeOut()).isEqualTo(Duration.ofSeconds(5));
    }

}