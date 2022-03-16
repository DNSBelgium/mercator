package be.dnsbelgium.mercator.geoip;

import org.apache.commons.lang3.tuple.Pair;

import java.net.InetAddress;
import java.util.Optional;

public class DisabledGeoIPService implements GeoIPService {

  @Override
  public Optional<String> lookupCountry(String ip) {
    return Optional.empty();
  }

  @Override
  public Optional<String> lookupCountry(InetAddress addr) {
    return Optional.empty();
  }

  @Override
  public Optional<Pair<Integer, String>> lookupASN(InetAddress ip) {
    return Optional.empty();
  }

  @Override
  public Optional<Pair<Integer, String>> lookupASN(String ip) {
    return Optional.empty();
  }
}
