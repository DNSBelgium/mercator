package be.dnsbelgium.mercator.geoip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.slf4j.LoggerFactory.getLogger;

@EnabledIfEnvironmentVariable(named = "MAXMIND_LICENSE_KEY", matches = ".*", disabledReason = "No MAXMIND_LICENSE_KEY set")
public class GeoIPServiceTest {

  private GeoIPServiceImpl geoIPService;
  private MaxMindConfig config;

  @TempDir
  protected static Path maxMindPath;
  private static String location;
  private static final Logger logger = getLogger(GeoIPServiceTest.class);


  @BeforeEach
  public void before() {
    location = maxMindPath.toString();
    logger.info("location = {}", location);
    config = MaxMindConfig.free(Duration.ofDays(1), System.getenv("MAXMIND_LICENSE_KEY"), location);
    System.out.println("System.getenv(\"MAXMIND_LICENSE_KEY\") = " + System.getenv("MAXMIND_LICENSE_KEY"));
    geoIPService = new GeoIPServiceImpl(config);
  }

  @Test
  public void initialize() {
    File folder = new File(location);
    logger.info("folder = {}", folder);
    FileSystemUtils.deleteRecursively(folder);
    assertThat("maxmind folder does not exist", !folder.exists());
    geoIPService = new GeoIPServiceImpl(config);
    assertThat("maxmind folder exists", folder.exists());
    assertThat("maxmind folder is a folder", folder.isDirectory());
    File asnFile = new File(location, "GeoLite2-ASN.mmdb");
    File countryFile = new File(location, "GeoLite2-ASN.mmdb");
    assertThat("ASN file exists", countryFile.exists());
    assertThat("country file exists", asnFile.exists());
  }

  @Test
  public void lookupASN() {
    var asn = geoIPService.lookupASN("8.8.8.8");
    assertThat("Found the ASN", asn, notNullValue());
    assertThat("Found the ASN", asn.isPresent());
    assertThat("ASN", asn.get().getLeft(), is(15169L));
    assertThat("Org is google", asn.get().getRight(), containsStringIgnoringCase("Google"));
  }

  @Test
  public void lookupCountry() {
    Optional<String> country = geoIPService.lookupCountry("8.8.8.8");
    assertThat("Found the country", country, notNullValue());
    assertThat("country is present", country.isPresent());
    assertThat("country", country.get(), is("US"));
  }

  @Test
  public void localhost() {
    Optional<String> country = geoIPService.lookupCountry("127.0.0.1");
    assertThat("country is empty", country.isEmpty());
  }

  @Test
  public void invalidIP() {
    Optional<String> country = geoIPService.lookupCountry("this is not a valid IP address");
    assertThat("country is empty", country.isEmpty());
    var asn = geoIPService.lookupASN("neither is this");
    assertThat("ASN is empty", asn.isEmpty());
  }

}
