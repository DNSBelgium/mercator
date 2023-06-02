package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.geoip.GeoIPService;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.test.RedisContainer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
    private static final RedisContainer redisContainer = new RedisContainer();

    @BeforeEach
    public void init() {
        analyzer = new DefaultSmtpIpAnalyzer(meterRegistry, conversationFactory, geoIPService,  1, redisContainer.getHost(), redisContainer.getPort());
        JedisPool jedisPool = new JedisPool(redisContainer.getHost(), redisContainer.getPort());
        try (Jedis jedis = jedisPool.getResource()){
            jedis.flushDB();
        }
    }

    @Test
    public void testCaching() {
        InetAddress ip1 = ip("10.20.30.40");
        InetAddress ip2 = ip("10.66.66.66");
        ISmtpConversation conversation1 = mock(ISmtpConversation.class);
        ISmtpConversation conversation2 = mock(ISmtpConversation.class);
        ISmtpConversation conversation3 = mock(ISmtpConversation.class);

        when(conversation1.talk()).thenReturn(new SmtpConversation(ip1));
        when(conversation2.talk()).thenReturn(new SmtpConversation(ip2));

        when(conversationFactory.create(ip1)).thenReturn(conversation1).thenReturn(conversation3);
        when(conversationFactory.create(ip2)).thenReturn(conversation2);

        SmtpConversation one = analyzer.crawl(ip1);
        SmtpConversation two = analyzer.crawl(ip2);
        SmtpConversation three = analyzer.crawl(ip1);

        logger.info("one   = {}", one);
        logger.info("two   = {}", two);
        logger.info("three = {}", three);

        assertThat(one).isEqualTo(three);
        assertThat(one).isNotEqualTo(two);

        verify(conversation1, times(1)).talk();
        verify(conversation2, times(1)).talk();
        // conversation1 was cached and is returned.
        verify(conversation3, never()).talk();

        assertThat(meterRegistry.counter(MetricName.COUNTER_CACHE_HITS).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(MetricName.COUNTER_CACHE_MISSES).count()).isEqualTo(2.0);
    }

    @Test
    public void geoIP() {
        InetAddress ip = ip("10.20.30.40");
        when(geoIPService.lookupASN(ip.getHostAddress())).thenReturn(Optional.of(Pair.of(123, "ASN 123")));
        when(geoIPService.lookupCountry(ip.getHostAddress())).thenReturn(Optional.of("Test Country"));
        ISmtpConversation conversation1 = mock(ISmtpConversation.class);
        when(conversationFactory.create(ip)).thenReturn(conversation1);
        when(conversation1.talk()).thenReturn(new SmtpConversation(ip));
        SmtpConversation found = analyzer.crawl(ip);
        logger.info("found = {}", found);
        assertThat(found.getAsn()).isEqualTo(123);
        assertThat(found.getAsnOrganisation()).isEqualTo("ASN 123");
        assertThat(found.getCountry()).isEqualTo("Test Country");
    }
}
