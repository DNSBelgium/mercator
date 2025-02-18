package be.dnsbelgium.mercator.smtp.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpHost {

  private String id;

  private boolean fromMx;

  private String hostName;

  private int priority;

  private SmtpConversation conversation;

}
