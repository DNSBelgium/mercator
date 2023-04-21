package be.dnsbelgium.mercator.smtp.persistence.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SmtpHostEntityId implements Serializable {
  private UUID visitId;
  private String hostName;
  private Long conversationId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SmtpHostEntityId that = (SmtpHostEntityId) o;
    return Objects.equals(visitId, that.visitId) && Objects.equals(hostName, that.hostName) && Objects.equals(conversationId, that.conversationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(visitId, hostName, conversationId);
  }
}
