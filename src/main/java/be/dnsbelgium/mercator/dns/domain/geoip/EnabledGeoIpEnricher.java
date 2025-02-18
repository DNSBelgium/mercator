package be.dnsbelgium.mercator.dns.domain.geoip;

import be.dnsbelgium.mercator.dns.metrics.MetricName;
import be.dnsbelgium.mercator.dns.dto.ResponseGeoIp;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public class EnabledGeoIpEnricher implements GeoIpEnricher {

  private static final Logger log = LoggerFactory.getLogger(EnabledGeoIpEnricher.class);

  private final GeoIPService geoIPService;

  public EnabledGeoIpEnricher(GeoIPService geoIPService) {
    this.geoIPService = geoIPService;
  }

  @Timed(MetricName.GEO_ENRICH)
  public ResponseGeoIp enrich(InetAddress ip) {
    int ipVersion = getIpVersion(ip);

    String country = geoIPService.lookupCountry(ip).orElse(null);
    Pair<Long, String> asn = geoIPService.lookupASN(ip).orElse(null);

    if (country != null || asn != null) {
      return new ResponseGeoIp(asn, country, ipVersion, ip.getHostAddress());
    }
    return null;
  }

  private int getIpVersion(InetAddress ip) {
    int ipVersion;
    if (ip instanceof Inet6Address) {
      ipVersion = 6;
    } else if (ip instanceof Inet4Address) {
      ipVersion = 4;
    } else {
      log.error("InetAddress isn't ipv4 nor ipv6");
      throw new IllegalArgumentException("InetAddress isn't ipv4 nor ipv6");
    }
    return ipVersion;
  }
}
