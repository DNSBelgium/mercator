package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class HtmlReader {

  private static final Logger logger = getLogger(HtmlReader.class);

  private final AmazonS3 amazonS3;

  public HtmlReader(AmazonS3 amazonS3) {
    this.amazonS3 = amazonS3;
  }

  InputStream read(ContentCrawlResult crawlResult) {
    return contentsOf(crawlResult.getBucket(), crawlResult.getHtmlKey());
  }

  private InputStream contentsOf(String bucketName, String key) {
    logger.debug("Reading contents of s3://{}/{}", bucketName, key);
    // We don't use an SimpleStorageProtocolResolver to read resources from S3
    // since Spring Cloud AWS folks like to change their implementation and make it really hard to use LocalStack
    // see https://github.com/spring-cloud/spring-cloud-aws/issues/641
    // and https://github.com/spring-cloud/spring-cloud-aws/issues/348

    // javadoc for getObject says "Be extremely careful when using this method;"
    // TODO: compare performance with getObjectAsString
    S3Object s3Object = amazonS3.getObject(bucketName, key);
    return s3Object.getObjectContent();
  }

}
