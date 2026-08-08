package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.tls.domain.TlsVisit;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import com.github.f4b6a3.ulid.Ulid;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.Security;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SpringBootTest
public class QuickTest {

  private static final Logger logger = getLogger(QuickTest.class);

  @Autowired
  TlsCrawler tlsCrawler;

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

  @Disabled
  @Test // compare results of ssllabs protocols with these to confirm
  public void testHasSSL2OrSSL3() {
    List<String> domainNames = Arrays.asList(
            "www.afhaalmaaltijden.be",
            "aagdc.be",
            "www.aalst-carnaval.be",
            "festivalindestad.be",
            "www.aagdc.be",
            "fiell.be",
            "www.fiell.be",
            "www.onestepbeyond.be",
            "onestepbeyond.be",
            "agimex.be",
            "www.agimex.be",
            "online-schaken.be",
            "www.online-schaken.be",
            "agrolink.be",
            "fiamairtools.be",
            "www.fiamairtools.be",
            "firatspuitcabines.be",
            "www.firatspuitcabines.be",
            "openbareverlichting.be",
            "www.agrolink.be",
            "aalst-carnaval.be",
            "ouderraaddekoepel.be",
            "www.ouderraaddekoepel.be",
            "fleursdesdames.be",
            "www.fleursdesdames.be",
            "fiori.be",
            "www.fiori.be",
            "ahha.be",
            "www.ahha.be",
            "openvldzone.be",
            "www.openvldzone.be",
            "fiserco.be",
            "www.fiserco.be",
            "fleuropevents.be",
            "www.fleuropevents.be",
            "dnsbelgium.be"
    );

    List<Map<String, Object>> tableData = new ArrayList<>();

    for (String domainName : domainNames) {
      VisitRequest visitRequest = new VisitRequest(Ulid.fast().toString(), domainName);
      TlsVisit tlsVisit = tlsCrawler.visit(visitRequest, "");

      boolean supportsSsl2 = tlsVisit.getFullScanEntity().isSupportSsl_2_0();
      boolean supportsSsl3 = tlsVisit.getFullScanEntity().isSupportSsl_3_0();

      logger.info("Domain: " + domainName + " | SSL2: " + supportsSsl2 + " | SSL3: " + supportsSsl3);

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("Domain", domainName);
      row.put("Supports SSL2", supportsSsl2);
      row.put("Supports SSL3", supportsSsl3);
      tableData.add(row);
    }

    System.out.println(String.format("%-30s %-10s %-10s", "Domain,", "SSL2,", "SSL3,"));

    for (Map<String, Object> row : tableData) {
      System.out.println(String.format("%-30s %-10s %-10s",
              row.get("Domain")+ ",", row.get("Supports SSL2")+ ",", row.get("Supports SSL3")+ ","));
    }

  }
}
