package be.dnsbelgium.mercator.smtp.domain.crawler;

import java.net.InetAddress;

public interface SmtpConversationFactory {

    ISmtpConversation create(InetAddress ip);

}
