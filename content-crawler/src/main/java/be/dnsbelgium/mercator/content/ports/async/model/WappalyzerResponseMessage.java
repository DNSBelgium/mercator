package be.dnsbelgium.mercator.content.ports.async.model;

import be.dnsbelgium.mercator.content.dto.wappalyzer.WappalyzerReport;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false) // Remove warning from lombok
public class WappalyzerResponseMessage extends ResponseMessage {

  private WappalyzerRequestMessage request;
  private WappalyzerReport wappalyzer;

}
