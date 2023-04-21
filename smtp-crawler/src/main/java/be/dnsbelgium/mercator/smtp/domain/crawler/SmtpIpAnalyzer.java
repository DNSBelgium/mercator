package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;

import java.net.InetAddress;

/**
 * Analyzes the SMTP server on one given IP
 */
public interface SmtpIpAnalyzer {

    SmtpConversation crawl(InetAddress ip);
}
