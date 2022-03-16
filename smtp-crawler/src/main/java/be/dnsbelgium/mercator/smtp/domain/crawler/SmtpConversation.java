package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;

public interface SmtpConversation {

    SmtpHostIp talk();
}
