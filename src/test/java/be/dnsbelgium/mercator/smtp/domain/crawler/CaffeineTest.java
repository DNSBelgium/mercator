package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.SmtpTestUtils;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static be.dnsbelgium.mercator.smtp.SmtpTestUtils.ip;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

// Quick test to get to know Caffeine and its TTL
@Tag("fast")
public class CaffeineTest {

    private static final Logger logger = getLogger(CaffeineTest.class);
    public static final int CACHE_TIME = 10;

    private SmtpConversation findByIp(InetAddress ip) {
        logger.info("crawling for ip = {}", ip);
        SmtpConversation smtpConversation = new SmtpConversation(ip);
        smtpConversation.setBanner("banner for " + ip);
        return smtpConversation;
    }

    @Test
    public void testMissAndHit() {
        LoadingCache<InetAddress, SmtpConversation> cache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TIME, TimeUnit.MILLISECONDS)
                .maximumSize(100)
                .recordStats()
                .build(this::findByIp);

        InetAddress ip = ip("10.20.30.40");

        // first miss
        SmtpConversation foundNothing = cache.getIfPresent(ip);
        assertThat(foundNothing).isNull();

        // second miss but it populates the cache
        SmtpConversation cacheMiss = cache.get(ip);
        assertThat(cacheMiss).isNotNull();
        assertThat(cacheMiss.getBanner()).isEqualTo("banner for " + ip);

        // first hit
        SmtpConversation cacheHit = cache.get(ip);
        assertThat(cacheHit).isNotNull();
        assertThat(cacheHit).isEqualTo(cacheMiss);

        SmtpTestUtils.sleep(CACHE_TIME);
        // third miss
        SmtpConversation evicted = cache.getIfPresent(ip);
        assertThat(evicted).isNull();

        assertThat(cache.stats().hitCount()).isEqualTo(1);
        assertThat(cache.stats().missCount()).isEqualTo(3);
    }


}
