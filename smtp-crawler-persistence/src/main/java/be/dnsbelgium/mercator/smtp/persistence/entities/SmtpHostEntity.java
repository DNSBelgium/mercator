package be.dnsbelgium.mercator.smtp.persistence.entities;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
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

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "conversation")
  @ToString.Exclude
  private SmtpConversationEntity conversation;

  public SmtpHostEntity(String hostName){
    this.hostName = hostName;
  }

  public void setConversation(SmtpConversationEntity conversation) {
    this.conversation = conversation;
  }
}
