package be.dnsbelgium.mercator.smtp.persistence.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(SmtpHostEntityId.class)
@Table(name = "smtp_host")
public class SmtpHostEntity {

  @Id
  @Column(name = "visit_id")
  private UUID visitId;

  @ManyToOne
  @JoinColumn(name = "visit_id")
  @MapsId("visitId")
  private SmtpVisitEntity visit;

  @Column(name = "from_mx")
  private boolean fromMx;

  @Id
  @Column(name = "host_name")
  private String hostName;

  private int priority;

  @Id
  @Column(name = "conversation")
  private Long conversationId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "conversation")
  @MapsId("conversationId")
  private SmtpConversationEntity conversation;
}
