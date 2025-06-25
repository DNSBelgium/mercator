package be.dnsbelgium.mercator.smtp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.f4b6a3.ulid.Ulid;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmtpVisit {

  private String visitId;

  private String domainName;

  @Builder.Default
  private Instant timestamp = Instant.now();

  @ToString.Exclude
  @Builder.Default
  private List<SmtpHost> hosts = new ArrayList<>();

  private CrawlStatus crawlStatus;

  public void add(SmtpHost host) {
    hosts.add(host);
  }

  public int getNumHosts() {
    return this.hosts.size();
  }

  public int getNumConversations() {
      return hosts != null
        ? hosts.stream()
               .filter(h -> h.getConversations() != null)
               .mapToInt(h -> h.getConversations().size())
               .sum()
        : 0;
  }

  public static String generateVisitId() {
    return Ulid.fast().toString();
  }

}
