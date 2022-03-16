package be.dnsbelgium.mercator.geoip;

import org.apache.commons.lang3.tuple.Pair;

import java.net.InetAddress;
import java.util.Optional;

public interface GeoIPService {

  Optional<String> lookupCountry(String ip);

  Optional<String> lookupCountry(InetAddress addr);

  Optional<Pair<Integer, String>> lookupASN(InetAddress ip);

  Optional<Pair<Integer, String>> lookupASN(String ip);

}
