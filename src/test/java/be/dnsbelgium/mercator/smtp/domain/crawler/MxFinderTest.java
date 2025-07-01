package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class MxFinderTest {

    private static final Logger logger = getLogger(MxFinderTest.class);

    private MxFinder mxFinder;

    @BeforeEach
    public void init() throws UnknownHostException {
        mxFinder = new MxFinder(2, 500, true);
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
        MxLookupResult result = mxFinder.findMxRecordsFor("dnssec-failed.org.");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.QUERY_FAILED);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
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
