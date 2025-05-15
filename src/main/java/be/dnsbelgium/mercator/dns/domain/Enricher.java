package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.dto.Request;
import be.dnsbelgium.mercator.dns.dto.Response;
import be.dnsbelgium.mercator.dns.dto.ResponseGeoIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static be.dnsbelgium.mercator.dns.dto.RecordType.*;

@Component
public class Enricher {

  private final GeoIpEnricher geoIpEnricher;
  private static final Logger logger = LoggerFactory.getLogger(Enricher.class);

  public Enricher(GeoIpEnricher geoIpEnricher) {
    this.geoIpEnricher = geoIpEnricher;
  }

  public void enrichResponses(List<Request> requests) {
    for (Request request : requests) {
      for (Response response : request.getResponses()) {
        if (shouldEnrich(request)) {
          String host_or_ip = response.getRecordData();
          List<ResponseGeoIp> enrichments = enrich(host_or_ip);
          response.getResponseGeoIps().addAll(enrichments);
        }
      }
    }
  }

  public boolean shouldEnrich(Request request) {
    var type = request.getRecordType();
    return (type == A || type == AAAA || type == NS);
  }

  public List<ResponseGeoIp> enrich(String host_or_ip) {
    var result = new ArrayList<ResponseGeoIp>();
    try {
      InetAddress[] ips = InetAddress.getAllByName(host_or_ip);
      for (InetAddress ip : ips) {
        ResponseGeoIp enriched = geoIpEnricher.enrich(ip);
        if (enriched != null)
          result.add(enriched);
      }
    } catch (UnknownHostException e) {
      // TODO: should we log this? Does this also happen when hostname is valid but does not exist?
      logger.warn("hostName or IP found in rdata is invalid: {}", host_or_ip);
    }
    return result;
  }
  
}
