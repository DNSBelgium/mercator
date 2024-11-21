package be.dnsbelgium.mercator.tls;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class QuickTest {

  private static final Logger logger = getLogger(QuickTest.class);

  @Test
  public void test() throws IOException {

    String disabled = Security.getProperty("jdk.tls.disabledAlgorithms");
    logger.info("disabled = {}", disabled);

    SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    SSLSocket soc = (SSLSocket) factory.createSocket();
    soc.setEnabledProtocols(new String[]{"SSLv3"});
    String[] protocols = soc.getEnabledProtocols();

    //soc.setA

    String[] ciphers = soc.getEnabledCipherSuites();
    logger.info("ciphers = {}", Arrays.toString(ciphers));

    //soc.startHandshake();


    List<String> enabledProtocols = new ArrayList<>(List.of(protocols));
    assertThat(enabledProtocols).containsExactly("SSLv3");
  }
}
