package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpHostIp;

import java.net.InetAddress;

/**
 * Analyzes the SMTP server on one given IP
 */
public interface SmtpIpAnalyzer {

    SmtpHostIp crawl(InetAddress ip);
}
