package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

// "Do not run on Jenkins. This test is brittle since it relies on external state"
@EnabledOnOs(OS.MAC)
class MxFinderTest {

    private static boolean avoidInternet() {
        return true;
    }

    private static final Logger logger = getLogger(MxFinderTest.class);

    private MxFinder mxFinder;

    @BeforeEach
    public void init() throws UnknownHostException {
        mxFinder = new MxFinder("8.8.8.8", 2, 2500, true);
    }

    @Test
    @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
    public void dnsbelgium() {
        MxLookupResult result = mxFinder.findMxRecordsFor("dnsbelgium.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.OK);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isGreaterThan(0);
    }

    @Test
    @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
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

    @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
    @Test
    public void manyMxRecords() {
        MxLookupResult result = mxFinder.findMxRecordsFor("kuleuven.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.OK);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isGreaterThan(1);
    }

    @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
    @Test
    public void noMxRecords() {
        MxLookupResult result = mxFinder.findMxRecordsFor("dc3.be");
        logger.info("result = {}", result);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MxLookupResult.Status.NO_MX_RECORDS_FOUND);
        assertThat(result.getMxRecords()).isNotNull();
        assertThat(result.getMxRecords().size()).isEqualTo(0);
    }

    @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
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
