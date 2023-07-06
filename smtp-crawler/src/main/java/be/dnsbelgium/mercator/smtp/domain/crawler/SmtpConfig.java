package be.dnsbelgium.mercator.smtp.domain.crawler;

import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("smtp.crawler")
@ToString
@Getter
public class SmtpConfig {

// see https://tools.ietf.org/html/rfc5321#section-4.5.3.2.1
// Wait 5 minutes for initial 220 Message
// An SMTP client process needs to distinguish between a failed TCP connection and a delay in receiving
// the initial 220 greeting message.
// Many SMTP servers accept a TCP connection but delay delivery of the  220 message until their system load permits
// more mail to be processed.

  public static final String DEFAULT_INITIAL_RESPONSE_TIME_OUT = "5m";
  public static final String DEFAULT_READ_TIME_OUT = "3m";
  public static final String DEFAULT_EHLO_DOMAIN = "smtp.crawler";

  private final String ehloDomain;
  private final int numThreads;
  private final Duration initialResponseTimeOut;
  private final Duration readTimeOut;
  private final int smtpPort;
  private final boolean logStackTraces;
  private final boolean trustAnyone;

  @ConstructorBinding
  public SmtpConfig(
      @DefaultValue(DEFAULT_EHLO_DOMAIN) String ehloDomain,
      @DefaultValue("1") int numThreads,
      @DefaultValue(DEFAULT_READ_TIME_OUT) Duration readTimeOut,
      @DefaultValue(DEFAULT_INITIAL_RESPONSE_TIME_OUT) Duration initialResponseTimeOut,
      @DefaultValue("25") int smtpPort,
      @DefaultValue("false") boolean logStackTraces,
      @DefaultValue("false") boolean trustAnyone
  ) {
    this.ehloDomain = ehloDomain;
    this.numThreads = numThreads;
    this.initialResponseTimeOut = initialResponseTimeOut;
    this.readTimeOut = readTimeOut;
    this.logStackTraces = logStackTraces;
    this.trustAnyone = trustAnyone;
    this.smtpPort = smtpPort;
  }

  public static SmtpConfig testConfig() {
    return new SmtpConfig(
        DEFAULT_EHLO_DOMAIN,
        1,
        Duration.ofMinutes(3),
        Duration.ofMinutes(5),
        25,
        true,
        true
    );
  }

}
