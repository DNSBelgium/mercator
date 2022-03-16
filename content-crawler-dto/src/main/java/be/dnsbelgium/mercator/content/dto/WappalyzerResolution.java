package be.dnsbelgium.mercator.content.dto;

import be.dnsbelgium.mercator.content.dto.wappalyzer.WappalyzerReport;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Value
public class WappalyzerResolution implements Resolution {

  UUID visitId;
  String domainName;
  String url;
  boolean ok;
  HashMap<String, WappalyzerReport.WappalyzerUrl> urls;
  List<WappalyzerReport.WappalyzerTechnology> technologies;
  String error;

}
