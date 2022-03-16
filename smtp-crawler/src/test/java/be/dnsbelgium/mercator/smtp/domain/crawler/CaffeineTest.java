package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.SmtpTestUtils;
import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;
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
    public static final int CACHE_TIME = 500;

    private SmtpHostIp findByIp(InetAddress ip) {
        logger.info("crawling for ip = {}", ip);
        SmtpHostIp smtpHostIp = new SmtpHostIp(ip);
        smtpHostIp.setBanner("banner for " + ip);
        return smtpHostIp;
    }

    @Test
    public void testMissAndHit() {
        LoadingCache<InetAddress, SmtpHostIp> cache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TIME, TimeUnit.MILLISECONDS)
                .maximumSize(100)
                .recordStats()
                .build(this::findByIp);

        InetAddress ip = ip("10.20.30.40");

        // first miss
        SmtpHostIp foundNothing = cache.getIfPresent(ip);
        assertThat(foundNothing).isNull();

        // second miss but it populates the cache
        SmtpHostIp cacheMiss = cache.get(ip);
        assertThat(cacheMiss).isNotNull();
        assertThat(cacheMiss.getBanner()).isEqualTo("banner for " + ip);

        // first hit
        SmtpHostIp cacheHit = cache.get(ip);
        assertThat(cacheHit).isNotNull();
        assertThat(cacheHit).isEqualTo(cacheMiss);

        SmtpTestUtils.sleep(CACHE_TIME);
        // third miss
        SmtpHostIp evicted = cache.getIfPresent(ip);
        assertThat(evicted).isNull();

        assertThat(cache.stats().hitCount()).isEqualTo(1);
        assertThat(cache.stats().missCount()).isEqualTo(3);
    }


}
