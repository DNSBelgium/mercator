package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.xbill.DNS.*;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class MxFinderTest {

    private static final Logger logger = getLogger(MxFinderTest.class);

    private MxFinder mxFinder;

    @BeforeEach
    public void init() throws UnknownHostException {
        mxFinder = new MxFinder(2, 500, true, null, 53);
    }

    @Test
    public void testResolverConfig() throws TextParseException {
        System.out.println(ResolverConfig.getCurrentConfig().servers());
        var resolver = new ExtendedResolver();
        logger.info("ExtendedResolver consists of {} resolvers", resolver.getResolvers().length);
        for (Resolver r: resolver.getResolvers()) {
            logger.info("ExtendedResolver uses = {}", r);
        }
        Lookup lookup = new Lookup(Name.fromString("abc.be"), Type.MX);
        lookup.setResolver(resolver);
        var records = lookup.run();
        logger.info("records = {}", (Object) records);
    }


    @Test
    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    public void dnsbelgium() {
        MxLookupResult result = mxFinder.findMxRecordsFor("dnsbelgium.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.OK);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isGreaterThan(0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    public void nxdomain() {
        MxLookupResult result = mxFinder.findMxRecordsFor("--.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.INVALID_HOSTNAME);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

    @Test
    public void empty() {
        MxLookupResult result = mxFinder.findMxRecordsFor("");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.INVALID_HOSTNAME);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    public void servfail() {
        String propBefore = System.getProperty("dns.server");
        logger.info("propBefore = {}", propBefore);
        // This test fails on GitHub when we don't explicitly set a dns.server (since it uses 127.0.0.53:53 which does not seem to do DNSSEC validation)
        // use a DNSSEC validating resolver
        System.setProperty("dns.server", "8.8.8.8");
        try {
            ResolverConfig.refresh();
            MxFinder mxFinder = new MxFinder(2, 500, true, null, 53);
            MxLookupResult result = mxFinder.findMxRecordsFor("dnssec-failed.org.");
            logger.info("result = {}", result);
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.QUERY_FAILED);
            assertThat(result.getMxRecords()).isNotNull();
            assertThat(result.getMxRecords().size()).isEqualTo(0);
        } finally {
            if (propBefore == null) {
                System.clearProperty("dns.server");
            } else {
                System.setProperty("dns.server", propBefore);
            }
            ResolverConfig.refresh();
        }
    }

    @Test
    public void nameTooLong() {
        MxLookupResult result = mxFinder.findMxRecordsFor(StringUtils.repeat("a", 65) + ".be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.INVALID_HOSTNAME);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    public void manyMxRecords() {
        MxLookupResult result = mxFinder.findMxRecordsFor("kuleuven.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.OK);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isGreaterThan(1);
    }

    @Test
    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    public void noMxRecords() {
        MxLookupResult result = mxFinder.findMxRecordsFor("dc3.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.NO_MX_RECORDS_FOUND);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

    @EnabledIfEnvironmentVariable(named="DNS_OUTBOUND_TESTS_ENABLED", matches = "true")
    @Test
    public void idn() {
        MxLookupResult result = mxFinder.findMxRecordsFor("sénat.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        logger.info(result.getStatus().toString());
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.NO_MX_RECORDS_FOUND);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

}
