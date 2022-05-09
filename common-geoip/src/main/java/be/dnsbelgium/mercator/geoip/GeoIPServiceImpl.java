/*
 * Copied from ENTRADA, a big data platform for network data analytics
 *
 * Copyright (C) 2016 SIDN [https://www.sidn.nl]
 *
 * This file is copied from ENTRADA.
 *
 * ENTRADA is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * ENTRADA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with ENTRADA. If not, see
 * [<http://www.gnu.org/licenses/].
 *
 */

package be.dnsbelgium.mercator.geoip;

import com.google.common.net.InetAddresses;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.IspResponse;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Utility class to lookup IP address information such as country and asn. Uses the maxmind database
 */
public class GeoIPServiceImpl implements GeoIPService {

  // free dbs
  private static final String FILENAME_GEOLITE_COUNTRY = "GeoLite2-Country.mmdb";
  private static final String FILENAME_GEOLITE_ASN = "GeoLite2-ASN.mmdb";
  // paid dbs
  private static final String FILENAME_GEOIP2_COUNTRY = "GeoIP2-Country.mmdb";
  private static final String FILENAME_GEOIP2_ASN = "GeoIP2-ISP.mmdb";

  private DatabaseReader geoReader;
  private DatabaseReader asnReader;

  private final MaxMindConfig config;

  private static final Logger log = LoggerFactory.getLogger(GeoIPServiceImpl.class);

  public GeoIPServiceImpl(MaxMindConfig config) {
    this.config = config;
    initialize();
  }

  private void initialize() {
    log.info("Using Maxmind database location: {}", config.getFileLocation());
    if (StringUtils.isBlank(config.getLicenseKey())) {
      throw new RuntimeException("No valid Maxmind license key found, provide key for either the free or paid license.");
    }
    File loc = new File(config.getFileLocation());
    if (!loc.exists()) {
      if (!loc.mkdirs()) {
        log.error("Failed to mkdirs {}", loc);
      }
    }

    String countryFile = countryFile();
    String asnFile = asnFile();

    if (shouldUpdate(countryFile)) {
      log.info("GEOIP country database does not exist or is too old, fetch latest version");
      String url = config.getUrlCountryDb() + config.getLicenseKey();
      if (config.isUsePaidVersion()) {
        log.info("Download paid Maxmind country database");
      }
      download(countryFile, url, 30);
    }

    if (shouldUpdate(asnFile)) {
      log.info("GEOIP ASN database does not exist or is too old, fetch latest version");
      String url = config.getUrlAsnDb() + config.getLicenseKey();
      if (config.isUsePaidVersion()) {
        log.info("Download paid Maxmind ISP database");
      }
      download(asnFile, url, 30);
    }

    try {
      // geo
      File database = new File(FileUtil.appendPath(config.getFileLocation(), countryFile));
      geoReader = new DatabaseReader.Builder(database).withCache(new CHMCache()).build();
      // asn
      database = new File(FileUtil.appendPath(config.getFileLocation(), asnFile));
      asnReader = new DatabaseReader.Builder(database).withCache(new CHMCache()).build();
    } catch (IOException e) {
      throw new RuntimeException("Error initializing Maxmind GEO/ASN database", e);
    }
  }

  private String countryFile() {
    return config.isUsePaidVersion() ? FILENAME_GEOIP2_COUNTRY : FILENAME_GEOLITE_COUNTRY;
  }

  private String asnFile() {
    return config.isUsePaidVersion() ? FILENAME_GEOIP2_ASN : FILENAME_GEOLITE_ASN;
  }

  /**
   * Check if the database should be updated
   *
   * @param database named of database
   * @return true if database file does not exist or is too old
   */
  private boolean shouldUpdate(String database) {
    File f = new File(FileUtil.appendPath(config.getFileLocation(), database));

    if (log.isDebugEnabled()) {
      log.debug("Check for file expiration for: {}", f);
    }
    if (!f.exists()) {
      log.info("File does not exist: {}", f);
      return true;
    }

    if (isExpired(f, config.getMaxFileAgeInDays())) {
      log.info("File is expired: {}", f);
      return true;
    }

    return false;
  }

  private boolean isExpired(File f, int maxDays) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -1 * maxDays);
    return isFileOlder(f, calendar.getTime());
  }

  public static boolean isFileOlder(final File file, final Date date) {
    if (date == null) {
      throw new IllegalArgumentException("No specified date");
    }
    return isFileOlder(file, date.getTime());
  }


  public static boolean isFileOlder(final File file, final long timeMillis) {
    if (file == null) {
      throw new IllegalArgumentException("No specified file");
    }
    if (!file.exists()) {
      return false;
    }
    return file.lastModified() < timeMillis;
  }

  @Override
  public Optional<String> lookupCountry(String ip) {
    InetAddress inetAddr;
    try {
      inetAddr = InetAddresses.forString(ip);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Invalid IP address: " + ip);
      }
      return Optional.empty();
    }
    return lookupCountry(inetAddr);
  }

  @Override
  public Optional<String> lookupCountry(InetAddress ip) {
    try {
      return Optional.ofNullable(geoReader.country(ip).getCountry().getIsoCode());
    } catch (AddressNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Maxmind error, IP not in database: " + ip);
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("No country found for: " + ip);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Pair<Integer, String>> lookupASN(InetAddress ip) {
    try {
      if (config.isUsePaidVersion()) {
        // paid version returns IspResponse
        IspResponse r = asnReader.isp(ip);
        return asn(r.getAutonomousSystemNumber(), r.getAutonomousSystemOrganization(), ip);
      }
      // use free version
      AsnResponse r = asnReader.asn(ip);
      return asn(r.getAutonomousSystemNumber(), r.getAutonomousSystemOrganization(), ip);
    } catch (AddressNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Maxmind error, IP not in database: " + ip);
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Error while doing ASN lookup for: " + ip);
      }
    }
    return Optional.empty();
  }

  public Optional<Pair<Integer, String>> asn(Integer asn, String org, InetAddress ip) {
    if (asn == null) {
      if (log.isDebugEnabled()) {
        log.debug("No asn found for: " + ip);
      }
      return Optional.empty();
    }
    return Optional.of(Pair.of(asn, org));
  }

  @Override
  public Optional<Pair<Integer, String>> lookupASN(String ip) {
    InetAddress inetAddr;
    try {
      inetAddr = InetAddresses.forString(ip);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Invalid IP address: " + ip);
      }
      return Optional.empty();
    }
    return lookupASN(inetAddr);
  }


  public void download(String database, String url, int timeout) {
    // do not log api key
    String logUrl = RegExUtils.removePattern(url, "&license_key=.+");
    Optional<byte[]> data = DownloadUtil.getAsBytes(url, logUrl, timeout);
    if (data.isPresent()) {
      InputStream is = new ByteArrayInputStream(data.get());
      try {
        extractDatabase(is, database);
      } catch (IOException e) {
        log.error("Error while extracting {}", database, e);
      }
    }
  }

  private void extractDatabase(InputStream in, String database) throws IOException {
    GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
    try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
      TarArchiveEntry entry;

      while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
        if (StringUtils.endsWith(entry.getName(), database)) {
          int count;
          byte[] data = new byte[4096];

          String outFile = Paths.get(entry.getName()).getFileName().toString();
          FileOutputStream fos =
              new FileOutputStream(FileUtil.appendPath(config.getFileLocation(), outFile), false);
          try (BufferedOutputStream dest = new BufferedOutputStream(fos, 4096)) {
            while ((count = tarIn.read(data, 0, 4096)) != -1) {
              dest.write(data, 0, count);
            }
          }
        }
      }
    }
  }

}
