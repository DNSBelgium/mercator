package be.dnsbelgium.mercator.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer {
  private final String host;
  private final int port;

  public RedisContainer(){
    GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7.0.11")).withExposedPorts(6379);
    redis.start();
    this.host = redis.getHost();
    this.port = redis.getMappedPort(6379);
    System.setProperty("smtp.crawler.ip.cache.host", host);
    System.setProperty("smtp.crawler.ip.cache.port", String.valueOf(port));

  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
