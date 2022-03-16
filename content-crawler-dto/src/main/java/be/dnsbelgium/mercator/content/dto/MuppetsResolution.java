package be.dnsbelgium.mercator.content.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class MuppetsResolution implements Resolution {

  UUID visitId;
  String domainName;
  String url;
  boolean ok;
  String errors;
  String finalUrl;
  String bucket;
  String screenshotFile;
  String htmlFile;
  Integer htmlLength;
  String harFile;
  String metrics;
  String ipv4;
  String ipv6;
  String browserVersion;

}
