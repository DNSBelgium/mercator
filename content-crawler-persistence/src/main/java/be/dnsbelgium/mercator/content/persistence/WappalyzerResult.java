package be.dnsbelgium.mercator.content.persistence;
import be.dnsbelgium.mercator.content.dto.WappalyzerResolution;
import be.dnsbelgium.mercator.content.dto.wappalyzer.WappalyzerReport;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.data.domain.AbstractAggregateRoot;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "wappalyzer_result")
public class WappalyzerResult extends AbstractAggregateRoot<WappalyzerResult> {

  @Id
  @Column(name = "visit_id")
  private UUID visitId;
  @Column(name = "domain_name")
  @Convert(converter = StatusJpaConverter.class)
  private String domainName;
  @Column(name = "url")
  private String url;
  @Column(name = "ok")
  private boolean ok;

  @Type(type = "jsonb")
  @Column(name = "urls", columnDefinition = "jsonb")
  private HashMap<String, WappalyzerReport.WappalyzerUrl> urls;

  @Type(type = "jsonb")
  @Column(name = "technologies", columnDefinition = "jsonb")
  private List<WappalyzerReport.WappalyzerTechnology> technologies;

  @Column(name = "error")
  private String error;

  public static WappalyzerResult of(WappalyzerResolution resolution) {
    return new WappalyzerResult(resolution.getVisitId(), resolution.getDomainName(), resolution.getUrl(),
            resolution.isOk(), resolution.getUrls(), resolution.getTechnologies(),
            resolution.getError());
  }

}
