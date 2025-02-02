package be.dnsbelgium.mercator.common;

import be.dnsbelgium.mercator.idn.IDN2008;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter @Setter @ToString
public class VisitRequest {

  private String visitId;
  private String domainName;

  public VisitRequest() {
  }

  public VisitRequest(String domainName) {
    this.domainName = domainName;
    this.visitId = VisitIdGenerator.generate();
  }

  public VisitRequest(String visitId, String domainName) {
    this.visitId = visitId;
    this.domainName = domainName;
  }

  public String u_label() {
    return IDN2008.toUnicode(domainName);
  }

  public String a_label() {
    return IDN2008.toASCII(domainName);
  }

}
