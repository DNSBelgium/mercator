package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Optional;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

class DefaultSmtpIpAnalyzerTest {

    private static final Logger logger = getLogger(DefaultSmtpIpAnalyzerTest.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GeoIPService geoIPService = mock(GeoIPService.class);
    private SmtpIpAnalyzer analyzer;
    private final SmtpConversationFactory conversationFactory = mock(SmtpConversationFactory.class);

    @BeforeEach
    public void init() {
        analyzer = new DefaultSmtpIpAnalyzer(meterRegistry, conversationFactory, geoIPService);
    }

    @Test
    public void geoInformationIsAdded() {
        InetAddress ip = ip("10.20.30.40");
        when(geoIPService.lookupASN(ip.getHostAddress())).thenReturn(Optional.of(Pair.of(123L, "ASN 123")));
        when(geoIPService.lookupCountry(ip.getHostAddress())).thenReturn(Optional.of("Test Country"));
        Conversation conversation1 = mock(Conversation.class);
        when(conversationFactory.create(ip)).thenReturn(conversation1);
        when(conversation1.talk()).thenReturn(new SmtpConversation(ip));
        SmtpConversation found = analyzer.crawl(ip);
        logger.info("found = {}", found);
        assertThat(found.getAsn()).isEqualTo(123);
        assertThat(found.getAsnOrganisation()).isEqualTo("ASN 123");
        assertThat(found.getCountry()).isEqualTo("Test Country");
    }
}
