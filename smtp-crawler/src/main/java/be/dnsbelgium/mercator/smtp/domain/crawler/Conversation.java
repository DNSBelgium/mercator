package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;

public interface Conversation {

    SmtpConversation talk();
}
