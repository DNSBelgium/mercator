package be.dnsbelgium.mercator.dns.domain.geoip;

import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class DisabledGeoIpEnricher implements GeoIpEnricher {

  @Override
  public ResponseGeoIp enrich(InetAddress ip) {
    return null;
  }

}
