package be.dnsbelgium.mercator.smtp.domain.crawler;

import java.net.InetAddress;

public interface SmtpConversationFactory {

    Conversation create(InetAddress ip);

}
