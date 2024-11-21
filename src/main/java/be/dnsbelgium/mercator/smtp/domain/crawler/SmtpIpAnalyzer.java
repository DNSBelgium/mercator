package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;

import java.net.InetAddress;

/**
 * Analyzes the SMTP server on one given IP
 */
public interface SmtpIpAnalyzer {

    SmtpConversation crawl(InetAddress ip);
}
