package be.dnsbelgium.mercator.dns.domain.geoip;

import be.dnsbelgium.mercator.dns.dto.ResponseGeoIp;

import java.net.InetAddress;

public interface GeoIpEnricher {

  ResponseGeoIp enrich(InetAddress ip);

}
