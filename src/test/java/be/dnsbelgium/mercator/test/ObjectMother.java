package be.dnsbelgium.mercator.test;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.tls.domain.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.domain.PageVisit;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import be.dnsbelgium.mercator.smtp.dto.CrawlStatus;
import okhttp3.HttpUrl;
import okhttp3.MediaType;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;

public class ObjectMother {

  private final HtmlFeatureExtractor htmlFeatureExtractor = new HtmlFeatureExtractor(new SimpleMeterRegistry(), false);

  Instant started  = LocalDateTime.of(2024,11,28, 23, 59).toInstant(ZoneOffset.UTC);

  public static String VISIT_ID_1 = "v104";
  public static String VISIT_ID_2 = "v105";

  public PageVisit pageVisit1() {
    return PageVisit.builder()
            .visitId(VISIT_ID_1)
            .html("<h1>I am a page </h1>")
            .url("https://www.dnsbelgium.be/")
            .bodyText("I am a page")
            .domainName("dnsbelgium.be")
            .statusCode(200)
            .path("/")
            .build();
  }

  public PageVisit pageVisit2() {
    return PageVisit.builder()
            .visitId(VISIT_ID_1)
            .html("<h1>I am the EN page </h1>")
            .url("https://www.dnsbelgium.be/en")
            .bodyText("I am the EN page")
            .domainName("dnsbelgium.be")
            .statusCode(200)
            .path("/en")
            .build();
  }

  public PageVisit pageVisit3() {
    return PageVisit.builder()
            .visitId(VISIT_ID_1)
            .html("<h1>I am the contact page </h1>")
            .url("https://www.dnsbelgium.be/en/contact")
            .bodyText("I am the contact page")
            .domainName("dnsbelgium.be")
            .vatValues(List.of("BE0466158640", "BE0841242495"))
            .statusCode(200)
            .path("/en/contact")
            .build();
  }

  public PageVisit pageVisit4() {
    return PageVisit.builder()
            .visitId(VISIT_ID_2)
            .html(null)
            .url("https://www.no-website.org/")
            .bodyText(null)
            .domainName("no-website.org")
            .statusCode(400)
            .path("/")
            .build();
  }

  public Page page1() {
    HttpUrl url = HttpUrl.parse("https://www.invalidpage123456.be/invalid-page");
    Instant visitStarted = Instant.now();
    Instant visitFinished = visitStarted.plusSeconds(10);
    String responseBody = "Bad Request: The page you are looking for could not be found.";
    long contentLength = responseBody.length();
    MediaType mediaType = MediaType.parse("text/html");
    Map<String, String> headers = Map.of("Content-Type", "text/html");

    return new Page(url, visitStarted, visitFinished, 400, responseBody, contentLength, mediaType, headers);
  }


  public PageVisit pageVisitWithSecurityTxtFields() {
    return PageVisit.builder()
            .visitId("security-txt-test-id")
            .url("https://www.example.org/.well-known/security.txt")
            .domainName("example.org")
            .path("/.well-known/security.txt")
            .statusCode(200)
            .bodyText("Contact: mailto:security@example.org\nEncryption: https://example.org/pgp-key.txt")
            .contentLength(128L)
            .headers(Map.of(
                    "Content-Type", "text/plain",
                    "Content-Length", "128"
            ))
            .build();
  }

  public HtmlFeatures htmlFeatures1() {
    HtmlFeatures features = htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am a page </h1>",
                    "https://www.dnsbelgium.be/",
                    "dnsbelgium.be");
    features.visitId = VISIT_ID_1;
    features.crawlTimestamp = started.plusMillis(11);
    features.domainName = "dnsbelgium.be";
    features.body_text_language = "nl";
    features.body_text_language_2 = "nl";
    return features;
  }

  public HtmlFeatures htmlFeatures2() {
    HtmlFeatures features =  htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am the English page </h1>",
                    "https://www.dnsbelgium.be/en",
                    "dnsbelgium.be");
    features.visitId = VISIT_ID_1;
    features.crawlTimestamp = started.plusMillis(12);
    features.domainName = "dnsbelgium.be";
    features.body_text_language = "en";
    features.body_text_language_2 = "en";
    return features;
  }

  public HtmlFeatures htmlFeatures3() {
    HtmlFeatures features =  htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am the Contact page </h1>",
                    "https://www.dnsbelgium.be/en/contact",
                    "dnsbelgium.be");
    features.visitId = VISIT_ID_1;
    features.crawlTimestamp = started.plusMillis(13);
    features.domainName = "dnsbelgium.be";
    features.body_text_language = "en";
    features.body_text_language_2 = "en";
    return features;
  }

  public WebCrawlResult webCrawlResult1() {
    return WebCrawlResult.builder()
            .visitId(VISIT_ID_1)
            .crawlStarted(started)
            .crawlFinished(started.plusMillis(235))
            .domainName("dnsbelgium.be")
            .matchingUrl("https://www.dnsbelgium.be/en/contact")
            .vatValues(List.of("BE0466158640", "BE0841242495"))
            .visitedUrls(List.of("https://www.dnsbelgium.be/", "https://www.dnsbelgium.be/en","https://www.dnsbelgium.be/en/contact"))
            .startUrl("https://www.dnsbelgium.be/")
            .htmlFeatures(List.of(htmlFeatures1(), htmlFeatures2(), htmlFeatures3()))
            .pageVisits(List.of(pageVisit1(), pageVisit2(), pageVisit3()))
            .detectedTechnologies(Set.of("Google Tag Manager", "Open Graph", "Drupal"))
            .build();

  }

  public WebCrawlResult webCrawlResult2() {
    return WebCrawlResult.builder()
            .crawlStarted(started.plusMillis(10))
            .crawlFinished(started.plusSeconds(115))
            .visitId(VISIT_ID_2)
            .domainName("no-website.org")
            .visitedUrls(List.of())
            .startUrl("https://www.no-website.be/")
            .htmlFeatures(List.of())
            .pageVisits(List.of(pageVisit4()))
            .detectedTechnologies(Set.of("HSTS", "Caddy", "Go"))
            .build();
  }

  public WebCrawlResult webCrawlResultWithPageVisitWithSecurityTxt() {
    return WebCrawlResult.builder()
            .crawlStarted(started.plusMillis(10))
            .crawlFinished(started.plusSeconds(115))
            .visitId(VISIT_ID_2)
            .domainName("no-website.org")
            .visitedUrls(List.of())
            .startUrl("https://www.no-website.be/")
            .htmlFeatures(List.of())
            .pageVisits(List.of(pageVisitWithSecurityTxtFields()))
            .detectedTechnologies(Set.of("HSTS", "Caddy", "Go"))
            .build();
  }


  public TlsCrawlResult tlsCrawlResult1()  {
    VisitRequest visitRequest = new VisitRequest("aakjkjkj-ojj", "example.org");
    return TlsCrawlResult.fromCache("www.tls.org", visitRequest, fullScanEntity("example.org"), singleVersionScan1());
  }

  public TlsCrawlResult tlsCrawlResult2()  {
    VisitRequest visitRequest = new VisitRequest("454545-54545", "example.be");
    return TlsCrawlResult.fromCache("www.example.org", visitRequest, fullScanEntity("example.org"), singleVersionScan2());
  }

  @SneakyThrows
  SingleVersionScan singleVersionScan1()  {
    Certificate certificate = Certificate.from(readTestCertificate("blackanddecker.be.pem"));
    SingleVersionScan singleVersionScan = SingleVersionScan.of(TlsProtocolVersion.TLS_1_0, new InetSocketAddress("abc.be", 443));
    singleVersionScan.setConnectOK(false);
    singleVersionScan.setErrorMessage("go away");
    singleVersionScan.setPeerCertificate(certificate);
    return singleVersionScan;
  }

  @SneakyThrows
  SingleVersionScan singleVersionScan2()  {
    Certificate certificate = Certificate.from(readTestCertificate("cll.be.pem"));
    SingleVersionScan singleVersionScan = SingleVersionScan.of(TlsProtocolVersion.TLS_1_3, new InetSocketAddress("cll.be", 443));
    singleVersionScan.setConnectOK(true);
    singleVersionScan.setPeerCertificate(certificate);
    return singleVersionScan;
  }


  public FullScanEntity fullScanEntity(String serverName) {
    return FullScanEntity.builder()
            .fullScanCrawlTimestamp(started)
            .ip("10.20.30.40")
            .serverName(serverName)
            .connectOk(true)
            .supportTls_1_3(false)
            .supportTls_1_2(true)
            .supportTls_1_1(true)
            .supportTls_1_0(true)
            .supportSsl_3_0(false)
            .supportSsl_2_0(false)
            .errorTls_1_0("Version not supported")
            .selectedCipherTls_1_2("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA")
            .selectedCipherTls_1_1("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384")
            .selectedCipherTls_1_0("")
            .lowestVersionSupported("TLSv1")
            .lowestVersionSupported("TLSv1.2")
            .millis_tls_1_0(10)
            .millis_tls_1_1(11)
            .millis_tls_1_2(12)
            .millis_tls_1_3(13)
            .millis_ssl_2_0(20)
            .millis_ssl_3_0(30)
            .build();
    };


  public SmtpConversation smtpConversationHost1() {
    return SmtpConversation.builder()
            .id("conv-host1")
            .ip("192.168.1.1")
            .asn(123456L)
            .asnOrganisation("ASN Org Host1")
            .banner("banner host1")
            .connectReplyCode(250)
            .country("US")
            .ipVersion(4)
            .startTlsOk(true)
            .connectOK(true)
            .timestamp(Instant.now())
            .software("software host1")
            .softwareVersion("1.0")
            .build();
  };
  public SmtpConversation smtpConversationHost2() {
    return SmtpConversation.builder()
            .id("conv-host2")
            .ip("192.168.1.2")
            .asn(789012L)
            .asnOrganisation("ASN Org Host2")
            .banner("banner host2")
            .connectReplyCode(220)
            .country("DE")
            .ipVersion(4)
            .startTlsOk(false)
            .connectOK(true)
            .timestamp(Instant.now())
            .software("software host2")
            .softwareVersion("2.0")
            .build();
  };


  public SmtpHost smtpHost1() {
    return SmtpHost.builder()
            .id("host1")
            .fromMx(true)
            .hostName("host1.example.com")
            .priority(10)
            .conversation(smtpConversationHost1())
            .build();
  };

  public SmtpHost smtpHost2() {
    return SmtpHost.builder()
            .id("host2")
            .fromMx(false)
            .hostName("host2.example.com")
            .priority(20)
            .conversation(smtpConversationHost2())
            .build();
  };

  public SmtpConversation smtpConversation1() {
        return SmtpConversation.builder()
            .numConversations(8)
            .id("ie-ze-fd-dfsdsf-d")
            .asn(4555454L)
            .ip("127.0.0.1")
            .asnOrganisation("asn org 1")
            .banner("banner 1")
            .connectionTimeMs(500)
            .connectReplyCode(555)
            .country("BE")
            .ipVersion(12)
            .startTlsOk(true)
            .connectOK(true)
            .timestamp(Instant.now())
            .errorMessage("error 1")
            .software("software 1")
            .softwareVersion("12")
            .startTlsReplyCode(455)
            .supportedExtensions(Set.of("test"))
            .visitId("visit-1234")
            .domainName("example.com")
            .crawlStatus(CrawlStatus.OK)
            .hosts(List.of(smtpHost1(), smtpHost2()))
            .build();
  }

}
