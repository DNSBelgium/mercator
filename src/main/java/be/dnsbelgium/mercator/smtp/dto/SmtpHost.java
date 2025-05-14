package be.dnsbelgium.mercator.smtp.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpHost {

  private boolean fromMx;

  private String hostName;

  private int priority;

  private List<SmtpConversation> conversations = new ArrayList<>();

}
