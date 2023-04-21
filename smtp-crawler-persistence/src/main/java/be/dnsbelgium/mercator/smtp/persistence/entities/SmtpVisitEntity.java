package be.dnsbelgium.mercator.smtp.persistence.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "smtp_visit")
public class SmtpVisitEntity {
  @Id
  @Column(name = "visit_id")
  private UUID visitId;

  @Column(name = "domain_name")
  private String domainName;

  private ZonedDateTime timestamp = ZonedDateTime.now();

  @Column(name = "num_conversations")
  private int numConversations;

  @OneToMany(mappedBy = "visit", fetch = FetchType.EAGER)
  private List<SmtpHostEntity> hosts;
}
