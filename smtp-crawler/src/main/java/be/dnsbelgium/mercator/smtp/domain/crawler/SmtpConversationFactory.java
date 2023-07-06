package be.dnsbelgium.mercator.smtp.domain.crawler;

import java.net.InetAddress;

public interface SmtpConversationFactory {

    SmtpConversation create(InetAddress ip);

}
