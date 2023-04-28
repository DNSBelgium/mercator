package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "smtp_host")
public class SmtpHostEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "visit_id")
  @ToString.Exclude
  private SmtpVisitEntity visit;

  @Column(name = "from_mx")
  private boolean fromMx;

  @Column(name = "host_name")
  private String hostName;

  private int priority;

  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "conversation")
  @ToString.Exclude
  private SmtpConversationEntity conversation;

  @Enumerated(EnumType.STRING)
  private HostStatus status;

  public SmtpHostEntity(String hostName){
    this.hostName = hostName;
  }

  public void setConversation(SmtpConversation conversation){
    this.conversation = new SmtpConversationEntity().fromSmtpConversation(conversation);
  }

  public void setConversation(SmtpConversationEntity conversation) {
    this.conversation = conversation;
  }
}
