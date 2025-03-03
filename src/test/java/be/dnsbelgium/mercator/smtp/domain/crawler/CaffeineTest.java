package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.SmtpTestUtils;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
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
    public static final int CACHE_TIME = 100;

    int crawlCount = 0;

    private SmtpConversation findByIp(InetAddress ip) {
        logger.info("crawling for ip = {}", ip);
        SmtpConversation smtpConversation = new SmtpConversation(ip);
        smtpConversation.setBanner("banner for " + ip);
        crawlCount++;
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
        crawlCount = 0;

        // first miss
        SmtpConversation foundNothing = cache.getIfPresent(ip);
        assertThat(foundNothing).isNull();
        assertThat(crawlCount).isEqualTo(0);

        // second miss but it populates the cache
        SmtpConversation cacheMiss = cache.get(ip);
        assertThat(cacheMiss).isNotNull();
        assertThat(cacheMiss.getBanner()).isEqualTo("banner for " + ip);
        logger.info("cache.estimatedSize() = {}", cache.estimatedSize());
        logger.info("cache.stats() = {}", cache.stats());
        assertThat(crawlCount).isEqualTo(1);

        // first hit
        SmtpConversation cacheHit = cache.get(ip);
        assertThat(cacheHit).isNotNull();
        assertThat(cacheHit).isEqualTo(cacheMiss);
        assertThat(crawlCount).isEqualTo(1);

        SmtpTestUtils.sleep(CACHE_TIME);
        // third miss
        SmtpConversation evicted = cache.getIfPresent(ip);
        assertThat(evicted).isNull();

        assertThat(cache.stats().hitCount()).isEqualTo(1);
        assertThat(cache.stats().missCount()).isEqualTo(3);
    }


}
