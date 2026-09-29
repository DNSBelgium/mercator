package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class Mailpit {

  public static final String MAILPIT_IMAGE_NAME = "axllent/mailpit:v1.20";

  public static GenericContainer<?> getMailPitContainer(boolean tls) {
    GenericContainer<?> container = new GenericContainer<>(MAILPIT_IMAGE_NAME)
            .withExposedPorts(1025, 8025);
    if (tls) {
      return container
              .withCopyToContainer(classpathResource("/test-certificates/smtp-tls-cert.pem"), "/var/mailpit.cert.pem")
              .withCopyToContainer(classpathResource("/test-certificates/smtp-tls-key.pem"), "/var/mailpit.key.pem")
              .withEnv("MP_SMTP_TLS_CERT", "/var/mailpit.cert.pem")
              .withEnv("MP_SMTP_TLS_KEY",  "/var/mailpit.key.pem");
    } else {
      return container;
    }
  }

  private static Transferable classpathResource(String path) {
    try (InputStream input = Mailpit.class.getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalArgumentException("Classpath resource does not exist: " + path);
      }
      return Transferable.of(input.readAllBytes(), 0644);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read classpath resource: " + path, e);
    }
  }
}
