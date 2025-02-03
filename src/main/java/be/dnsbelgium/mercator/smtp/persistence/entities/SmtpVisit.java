package be.dnsbelgium.mercator.smtp.persistence.entities;

import com.github.f4b6a3.ulid.Ulid;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmtpVisit {

  private String visitId;

  private String domainName;

  @Builder.Default
  private Instant timestamp = Instant.now();

  @Builder.Default
  private int numConversations = 0;

  @ToString.Exclude
  @Builder.Default
  private List<SmtpHost> hosts = new ArrayList<>();

  private CrawlStatus crawlStatus;

  public void add(SmtpHost host) {
    hosts.add(host);
    ++numConversations;
  }

  public static String generateVisitId() {
    return Ulid.fast().toString();
  }

}
