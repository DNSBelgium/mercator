package be.dnsbelgium.mercator.smtp.persistence.entities;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

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

  // TODO: check if we need this.
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

  public SmtpHostEntity(String hostName){
    this.hostName = hostName;
  }

  public void setConversation(SmtpConversation conversation){
    SmtpConversationEntity conversationEntity = new SmtpConversationEntity();
    conversationEntity.setFromSmtpConversation(conversation);
    this.conversation = conversationEntity;
  }

  public void setConversation(SmtpConversationEntity conversation) {
    this.conversation = conversation;
  }
}
