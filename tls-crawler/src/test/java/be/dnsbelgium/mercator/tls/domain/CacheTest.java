package be.dnsbelgium.mercator.tls.domain;

import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.jsr107.Eh107Configuration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import static org.slf4j.LoggerFactory.getLogger;

public class CacheTest {

  private static final Logger logger = getLogger(CacheTest.class);

  @Test
  public void test() {
    CachingProvider provider = Caching.getCachingProvider();
    CacheManager cacheManager = provider.getCacheManager();
    MutableConfiguration<Long, String> configuration =
        new MutableConfiguration<Long, String>()
            .setTypes(Long.class, String.class)
            .setStoreByValue(false)
            .setStatisticsEnabled(true)
//            .setReadThrough(true)
//            .setWriteThrough(true)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));
    Cache<Long, String> cache = cacheManager.createCache("jCache", configuration);
    cache.put(1L, "one");
    String value = cache.get(1L);
    CompleteConfiguration c = cache.getConfiguration(CompleteConfiguration.class);
    logger.info("c = {}", c.toString());
    logger.info("c = {}", c.isReadThrough());
    logger.info("c = {}", c.isWriteThrough());
    logger.info("c = {}", c.isStatisticsEnabled());
    logger.info("c = {}", c.getExpiryPolicyFactory());

    Eh107Configuration<Long, String> eh107Configuration = cache.getConfiguration(Eh107Configuration.class);
    CacheRuntimeConfiguration<Long, String> runtimeConfiguration = eh107Configuration.unwrap(CacheRuntimeConfiguration.class);

    logger.info("runtimeConfiguration = {}", runtimeConfiguration);
  }
}
