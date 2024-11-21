package be.dnsbelgium.mercator.geoip;

import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Configuration for MaxMind DB
 * <p>
 * MaxMind can be configured by setting these properties
 * <p>
 *  geo.ip.maxmind.max-file-age=5d
 *  geo.ip.maxmind.license-key=license-key
 *  geo.ip.maxmind.url-asn-db=http://localhost/asn-db
 *  geo.ip.maxmind.url-country-db=http://localhost/country-db
 *  geo.ip.maxmind.use-paid-version=false
 *  geo.ip.maxmind.file-location=
 * <p>
 * The only property that has no sensible default value is geo.ip.maxmind.license-key
 */
@SuppressWarnings("JavadocLinkAsPlainText")
@ConfigurationProperties("geo.ip.maxmind")
@ToString
@Getter
public class MaxMindConfig {

  public static final String DEFAULT_URL_FREE_ASN_DB = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-ASN&suffix=tar.gz&license_key=";
  public static final String DEFAULT_URL_FREE_COUNTRY_DB = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&suffix=tar.gz&license_key=";

  public static final String DEFAULT_URL_PAID_ASN_DB = "https://download.maxmind.com/app/geoip_download?edition_id=GeoIP2-ISP&suffix=tar.gz&license_key=";
  public static final String DEFAULT_URL_PAID_COUNTRY_DB = "https://download.maxmind.com/app/geoip_download?edition_id=GeoIP2-Country&suffix=tar.gz&license_key=";

  private final String urlCountryDb;
  private final String urlAsnDb;
  private final String licenseKey;
  private final String fileLocation;
  private final boolean usePaidVersion;

  @DurationUnit(ChronoUnit.DAYS)
  private final Duration maxFileAge;

  @ConstructorBinding
  public MaxMindConfig(
      @DefaultValue("1d")                         Duration maxFileAge,
      @DefaultValue(DEFAULT_URL_FREE_ASN_DB)      String urlAsnDb,
      @DefaultValue(DEFAULT_URL_FREE_COUNTRY_DB)  String urlCountryDb,
      @DefaultValue("false")                      boolean usePaidVersion,
      String licenseKey,
      String fileLocation
  ) {
    this.maxFileAge = maxFileAge;
    this.urlCountryDb = urlCountryDb;
    this.urlAsnDb = urlAsnDb;
    this.usePaidVersion = usePaidVersion;
    this.licenseKey = licenseKey;
    this.fileLocation = Objects.requireNonNullElse(fileLocation, getTempDir());
  }

  private String getTempDir() {
    return System.getProperty("java.io.tmpdir") + "/maxmind";
  }

  @SuppressWarnings("unused")
  public static MaxMindConfig paid(Duration maxFileAge, String licenseKey, String fileLocation) {
    return new MaxMindConfig(maxFileAge, DEFAULT_URL_PAID_ASN_DB, DEFAULT_URL_PAID_COUNTRY_DB, true, licenseKey, fileLocation);
  }

  public static MaxMindConfig free(Duration maxFileAge, String licenseKey, String fileLocation) {
    return new MaxMindConfig(maxFileAge, DEFAULT_URL_FREE_ASN_DB, DEFAULT_URL_FREE_COUNTRY_DB, false, licenseKey, fileLocation);
  }

  public int getMaxFileAgeInDays() {
    return (int) maxFileAge.toDays();
  }

}
