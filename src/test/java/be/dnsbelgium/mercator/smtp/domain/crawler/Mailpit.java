package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class Mailpit {

  public static final String MAILPIT_IMAGE_NAME = "axllent/mailpit:v1.20";

  public static GenericContainer<?> getMailPitContainer(boolean tls) {
    GenericContainer<?> container = new GenericContainer<>(MAILPIT_IMAGE_NAME)
            .withExposedPorts(1025, 8025);
    if (tls) {
      return container
              .withCopyToContainer(MountableFile.forClasspathResource("test-certificates/smtp-tls-cert.pem"), "/var/mailpit.cert.pem")
              .withCopyToContainer(MountableFile.forClasspathResource("test-certificates/smtp-tls-key.pem"), "/var/mailpit.key.pem")
              .withEnv("MP_SMTP_TLS_CERT", "/var/mailpit.cert.pem")
              .withEnv("MP_SMTP_TLS_KEY",  "/var/mailpit.key.pem");
    } else {
      return container;
    }
  }



}
