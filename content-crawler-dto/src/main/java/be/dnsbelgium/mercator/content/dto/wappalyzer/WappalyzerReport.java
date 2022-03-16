package be.dnsbelgium.mercator.content.dto.wappalyzer;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@Data
public class WappalyzerReport {

  private HashMap<String, WappalyzerUrl> urls;
  private List<WappalyzerTechnology> technologies;

  @Data
  public static class WappalyzerUrl implements Serializable {
    private int status;
    private String error;
  }

  @Data
  public static class WappalyzerTechnology {
    private String slug;
    private String name;
    private int confidence;
    private String version;
    private String icon;
    private String website;
    private String cpe;
    private List<WappalyzerCategory> categories;
  }

  @Data
  public static class WappalyzerCategory {
    private int id;
    private String slug;
    private String name;
  }
}
