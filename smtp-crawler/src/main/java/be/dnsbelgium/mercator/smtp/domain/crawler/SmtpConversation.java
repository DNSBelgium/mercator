package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;

public interface SmtpConversation {

    SmtpConversationEntity talk();
}
