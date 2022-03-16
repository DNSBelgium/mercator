package be.dnsbelgium.mercator.test;

import org.slf4j.Logger;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static org.slf4j.LoggerFactory.getLogger;

public class LocalstackContainer extends LocalStackContainer {

  private static final Logger logger = getLogger(LocalstackContainer.class);

  public LocalstackContainer() {
    super(DockerImageName.parse("localstack/localstack:0.14.0"));
    logger.info("dockerImageName = {}", getDockerImageName());
    withServices(Service.SQS);
    withServices(Service.S3);
    withReuse(true);
    withEnv("DEFAULT_REGION", "eu-west-1");
    start();
  }

  public void setDynamicPropertySource(DynamicPropertyRegistry registry) {
    registry.add("localstack.url", () -> getEndpointOverride(Service.SQS).toASCIIString());
    registry.add("cloud.aws.credentials.accessKey", this::getAccessKey);
    registry.add("cloud.aws.credentials.secretKey", this::getSecretKey);
  }

}
