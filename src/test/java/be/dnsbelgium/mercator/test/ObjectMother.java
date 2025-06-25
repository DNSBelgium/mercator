package be.dnsbelgium.mercator.test;

import be.dnsbelgium.mercator.dns.dto.*;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import be.dnsbelgium.mercator.tls.domain.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import be.dnsbelgium.mercator.tls.domain.*;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import be.dnsbelgium.mercator.vat.domain.Page;
import be.dnsbelgium.mercator.vat.domain.PageVisit;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import be.dnsbelgium.mercator.smtp.dto.CrawlStatus;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;

public class ObjectMother {

  private final HtmlFeatureExtractor htmlFeatureExtractor = new HtmlFeatureExtractor(new SimpleMeterRegistry(), false);

  Instant started = LocalDateTime.of(2024, 11, 28, 23, 59).toInstant(ZoneOffset.UTC);

  public static String VISIT_ID_1 = "v104";
  public static String VISIT_ID_2 = "v105";

  public PageVisit pageVisit1() {
    return PageVisit.builder()
        .responseBody("<h1>I am a page </h1>")
        .url("https://www.dnsbelgium.be/")
        .statusCode(200)
        .crawlStarted(started)
        .crawlStarted(started.plusMillis(312))
        .path("/")
        .build();
  }

  public PageVisit pageVisit2() {
    return PageVisit.builder()
        .responseBody("<h1>I am the EN page </h1>")
        .url("https://www.dnsbelgium.be/en")
        .statusCode(200)
        .crawlStarted(started)
        .crawlStarted(started.plusMillis(312))
        .path("/en")
        .build();
  }

  public PageVisit pageVisit3() {
    return PageVisit.builder()
        .responseBody("<h1>I am the contact page </h1>")
        .url("https://www.dnsbelgium.be/en/contact")
        .vatValues(List.of("BE0466158640", "BE0841242495"))
        .statusCode(200)
        .crawlStarted(started)
        .crawlStarted(started.plusMillis(312))
        .path("/en/contact")
        .build();
  }

  public PageVisit pageVisit4() {
    return PageVisit.builder()
        .responseBody(null)
        .url("https://www.no-website.org/")
        .statusCode(400)
        .crawlStarted(started)
        .crawlStarted(started.plusMillis(312))
        .path("/")
        .build();
  }

  public Page page1() {
    HttpUrl url = HttpUrl.parse("https://www.anything.be/invalid-page");
    Instant visitStarted = Instant.now();
    Instant visitFinished = visitStarted.plusSeconds(10);
    String responseBody = "Bad Request: The page you are looking for could not be found.";
    long contentLength = responseBody.length();
    MediaType mediaType = MediaType.parse("text/html");
    Map<String, List<String>> headers = Map.of("Content-Type", List.of("text/html"));
    assert url != null;
    return new Page(url, visitStarted, visitFinished, 400, responseBody, contentLength, mediaType, headers);
  }

  public Page page2() {
    HttpUrl url = HttpUrl.parse("https://www.anything.be/invalid-page");
    Instant visitStarted = Instant.now();
    Instant visitFinished = visitStarted.plusSeconds(10);
    String responseBody = "Bad Request: The page you are looking for could not be found.";
    long contentLength = responseBody.length();
    MediaType mediaType = MediaType.parse("text/html");
    Map<String, List<String>> headers = Map.of("Content-Type", List.of("text/html"));
    assert url != null;
    return new Page(url, visitStarted, visitFinished, 200, responseBody, contentLength, mediaType, headers);
  }

  public PageVisit pageVisitWithSecurityTxtFields() {
    return PageVisit.builder()
        .url("https://www.example.org/.well-known/security.txt")
        .path("/.well-known/security.txt")
        .statusCode(200)
        .responseBody("Contact: mailto:security@example.org\nEncryption: https://example.org/pgp-key.txt")
        .contentLength(128L)
        .headers(Map.of(
            "Content-Type", List.of("text/plain"),
            "Content-Length", List.of("128")
        ))
        .build();
  }

  public PageVisit pageVisitWithRobotsTxtFields() {
    return PageVisit.builder()
        .url("https://www.example.org/robots.txt")
        .path("/robots.txt")
        .statusCode(200)
        .responseBody("# # robots.txt # # This file is to prevent the crawling and indexing of certain parts #")
        .contentLength(128L)
        .headers(Map.of(
            "Content-Type", List.of("text/plain"),
            "Content-Length", List.of("128")
        ))
        .build();
  }

  public HtmlFeatures htmlFeatures1() {
    HtmlFeatures features = htmlFeatureExtractor
        .extractFromHtml(
            "<h1>I am a page </h1>",
            "https://www.dnsbelgium.be/",
            "dnsbelgium.be");
    features.body_text_language = "nl";
    features.body_text_language_2 = "nl";
    return features;
  }

  public HtmlFeatures htmlFeatures2() {
    HtmlFeatures features = htmlFeatureExtractor
        .extractFromHtml(
            "<h1>I am the English page </h1>",
            "https://www.dnsbelgium.be/en",
            "dnsbelgium.be");
    features.body_text_language = "en";
    features.body_text_language_2 = "en";
    return features;
  }

  public HtmlFeatures htmlFeatures3() {
    HtmlFeatures features = htmlFeatureExtractor
        .extractFromHtml(
            "<h1>I am the Contact page </h1>",
            "https://www.dnsbelgium.be/en/contact",
            "dnsbelgium.be");
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
        .visitedUrls(List.of("https://www.dnsbelgium.be/", "https://www.dnsbelgium.be/en", "https://www.dnsbelgium.be/en/contact"))
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
        .htmlFeatures(List.of())
        .pageVisits(List.of(pageVisitWithSecurityTxtFields()))
        .detectedTechnologies(Set.of("HSTS", "Caddy", "Go"))
        .build();
  }

  public WebCrawlResult webCrawlResultWithPageVisitWithRobotsTxt() {
    return WebCrawlResult.builder()
        .crawlStarted(started.plusMillis(10))
        .crawlFinished(started.plusSeconds(115))
        .visitId(VISIT_ID_2)
        .domainName("no-website.org")
        .visitedUrls(List.of())
        .htmlFeatures(List.of())
        .pageVisits(List.of(pageVisitWithRobotsTxtFields()))
        .detectedTechnologies(Set.of("HSTS", "Caddy", "Go"))
        .build();
  }

  public WebCrawlResult webCrawlResultWithNullValues() {
    return WebCrawlResult.builder()
        .visitId(null)
        .crawlStarted(null)
        .crawlFinished(null)
        .domainName(null)
        .matchingUrl(null)
        .vatValues(null)
        .visitedUrls(null)
        .htmlFeatures(null)
        .pageVisits(null)
        .detectedTechnologies(null)
        .build();

  }

  public WebCrawlResult webCrawlResultWithHtmlFeaturesNullValues() {
    return WebCrawlResult.builder()
        .visitId(null)
        .crawlStarted(null)
        .crawlFinished(null)
        .domainName(null)
        .matchingUrl(null)
        .vatValues(null)
        .visitedUrls(null)
        .htmlFeatures(List.of(htmlFeaturesWithNullValues()))
        .pageVisits(null)
        .detectedTechnologies(null)
        .build();

  }

  public WebCrawlResult webCrawlResultWithPageVisitNullValues() {
    return WebCrawlResult.builder()
        .visitId(null)
        .crawlStarted(null)
        .crawlFinished(null)
        .domainName(null)
        .matchingUrl(null)
        .vatValues(null)
        .visitedUrls(null)
        .htmlFeatures(null)
        .pageVisits(List.of(pageVisitWithNullValues()))
        .detectedTechnologies(null)
        .build();

  }


  public HtmlFeatures htmlFeaturesWithNullValues() {
    HtmlFeatures features = htmlFeatureExtractor
        .extractFromHtml(
            "",
            "https://www.no-website.be/",
            null);
    features.body_text_language = null;
    features.body_text_language_2 = null;
    return features;
  }

  public PageVisit pageVisitWithNullValues() {
    return PageVisit.builder()
        .responseBody(null)
        .url(null)
        .statusCode(null)
        .crawlStarted(null)
        .crawlStarted(null)
        .path(null)
        .build();
  }


  public TlsCrawlResult tlsCrawlResult1() {
    return getTlsCrawlResult("visit01", "tls.org", singleVersionScan1());
  }

  @NotNull
  private TlsCrawlResult getTlsCrawlResult(String visitId, String domainName, SingleVersionScan scan) {
    return new TlsCrawlResult(visitId, domainName, List.of(
        TlsVisit.fromCache(domainName, started, started.plusMillis(10), fullScanEntity(domainName), scan),
        TlsVisit.fromCache("www." + domainName, started, started.plusMillis(10), fullScanEntity(domainName), scan)
    ), Instant.parse("2025-03-28T12:00:00Z"), Instant.parse("2025-03-28T12:01:00Z"));
  }

  public TlsCrawlResult tlsCrawlResult2() {
    return getTlsCrawlResult("visit02", "example.be", singleVersionScan2());
  }

  public TlsCrawlResult tlsCrawlResult3() {
    return getTlsCrawlResult("visit03", "example.be", singleVersionScan2());
  }

  @SneakyThrows
  SingleVersionScan singleVersionScan1() {
    Certificate certificate = Certificate.from(readTestCertificate("blackanddecker.be.pem"));
    SingleVersionScan singleVersionScan = SingleVersionScan.of(TlsProtocolVersion.TLS_1_0, new InetSocketAddress("abc.be", 443));
    singleVersionScan.setConnectOK(false);
    singleVersionScan.setErrorMessage("go away");
    singleVersionScan.setPeerCertificate(certificate);
    singleVersionScan.setCertificateChain(List.of(certificate));
    return singleVersionScan;
  }

  @SneakyThrows
  SingleVersionScan singleVersionScan2() {
    Certificate certificate = Certificate.from(readTestCertificate("cll.be.pem"));
    Certificate certificate2 = Certificate.from(readTestCertificate("digicert-ca.pem"));
    SingleVersionScan singleVersionScan = SingleVersionScan.of(TlsProtocolVersion.TLS_1_3, new InetSocketAddress("cll.be", 443));
    singleVersionScan.setConnectOK(true);
    singleVersionScan.setHandshakeOK(true);
    singleVersionScan.setPeerCertificate(certificate);
    singleVersionScan.setCertificateChain(List.of(certificate, certificate2));
    return singleVersionScan;
  }


  public FullScanEntity fullScanEntity(String serverName) {
    return FullScanEntity.builder()
        .crawlStarted(started)
        .ip("10.20.30.40")
        .serverName(serverName)
        .connectOk(true)
        .supportTls_1_3(true)
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

  }

  public FullScanEntity fullScanEntityWithNullValues() {
    return FullScanEntity.builder()
        .crawlStarted(null)
        .ip(null)
        .serverName(null)
        .connectOk(false)
        .supportTls_1_3(false)
        .supportTls_1_2(false)
        .supportTls_1_1(false)
        .supportTls_1_0(false)
        .supportSsl_3_0(false)
        .supportSsl_2_0(false)
        .errorTls_1_0(null)
        .selectedCipherTls_1_2(null)
        .selectedCipherTls_1_1(null)
        .selectedCipherTls_1_0(null)
        .lowestVersionSupported(null)
        .lowestVersionSupported(null)
        .millis_tls_1_0(0)
        .millis_tls_1_1(0)
        .millis_tls_1_2(0)
        .millis_tls_1_3(0)
        .millis_ssl_2_0(0)
        .millis_ssl_3_0(0)
        .build();

  }

  private TlsCrawlResult getTlsCrawlResultWithFullScanEntityNull(SingleVersionScan scan) {
    return new TlsCrawlResult("visit02", "example.be", List.of(
        TlsVisit.fromCache("example.be", started, started.plusMillis(10), fullScanEntityWithNullValues(), scan),
        TlsVisit.fromCache("www." + "example.be", started, started.plusMillis(10), fullScanEntityWithNullValues(), scan)
    ), Instant.parse("2025-03-28T12:00:00Z"), Instant.parse("2025-03-28T12:01:00Z"));
  }

  public TlsCrawlResult tlsCrawlResultWithNullFullVersionEntityScan() {
    return getTlsCrawlResultWithFullScanEntityNull(singleVersionScan2());
  }

  public TlsCrawlResult tlsCrawlResultWithNullValues() {
    return new TlsCrawlResult(null, null, List.of(
        TlsVisit.fromCache(null, started, started.plusMillis(10), fullScanEntity(""), singleVersionScanWithNullValues()),
        TlsVisit.fromCache(null, started, started.plusMillis(10), fullScanEntity(""), singleVersionScanWithNullValues())
    ), Instant.parse("2025-03-28T12:00:00Z"), Instant.parse("2025-03-28T12:01:00Z"));
  }

  SingleVersionScan singleVersionScanWithNullValues() {
    SingleVersionScan singleVersionScan = SingleVersionScan.of(null, new InetSocketAddress("", 0));
    singleVersionScan.setConnectOK(false);
    singleVersionScan.setPeerCertificate(null);
    return singleVersionScan;
  }

  public SmtpVisit smtpVisit1() {
    return SmtpVisit.builder()
        .visitId("01HJR2Z6DZHS4G4P9X1BZSD4YV")
        .hosts(List.of(smtpHost1(), smtpHost2()))
        .domainName("example1.com")
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .crawlStatus(CrawlStatus.OK)
        .build();
  }

  public SmtpVisit smtpVisit2() {
    return SmtpVisit.builder()
        .visitId("02HJR2Z6DZHS4G4P9X1BZSD4YV")
        .hosts(List.of(smtpHost3(), smtpHost4()))
        .domainName("example2.com")
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .crawlStatus(CrawlStatus.OK)
        .build();
  }


  public SmtpConversation smtpConversation1() {
    return SmtpConversation.builder()
        .asn(12345L)
        .ip("192.168.1.1")
        .asnOrganisation("Example ISP")
        .banner("110 mail.example.com ESMTP Postfix")
        .connectionTimeMs(150)
        .connectReplyCode(220)
        .country("BE")
        .ipVersion(4)
        .startTlsOk(true)
        .connectOK(true)
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .errorMessage(null)
        .software("Postfix")
        .softwareVersion("3.5.8")
        .startTlsReplyCode(250)
        .supportedExtensions(Set.of("STARTTLS", "PIPELINING", "8BITMIME"))
        .build();
  }

  public SmtpConversation smtpConversation2() {
    return SmtpConversation.builder()
        .asn(12345L)
        .ip("192.168.1.2")
        .asnOrganisation("Example ISP")
        .banner("220 mail.example.com ESMTP Postfix")
        .connectionTimeMs(150)
        .connectReplyCode(220)
        .country("BE")
        .ipVersion(4)
        .startTlsOk(true)
        .connectOK(true)
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .errorMessage(null)
        .software("Postfix")
        .softwareVersion("3.5.8")
        .startTlsReplyCode(250)
        .supportedExtensions(Set.of("PIPELINING", "8BITMIME"))
        .build();
  }

  public SmtpConversation smtpConversation3() {
    return SmtpConversation.builder()
        .asn(12345L)
        .ip("192.168.1.3")
        .asnOrganisation("Example ISP")
        .banner("330 mail.example.com ESMTP Postfix")
        .connectionTimeMs(150)
        .connectReplyCode(220)
        .country("BE")
        .ipVersion(4)
        .startTlsOk(true)
        .connectOK(true)
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .errorMessage(null)
        .software("Postfix")
        .softwareVersion("3.5.8")
        .startTlsReplyCode(250)
        .supportedExtensions(Set.of("STARTTLS", "PIPELINING", "8BITMIME"))
        .build();
  }

  public SmtpConversation smtpConversation4() {
    return SmtpConversation.builder()
        .asn(12345L)
        .ip("192.168.1.4")
        .asnOrganisation("Example ISP")
        .banner("440 mail.example.com ESMTP Postfix")
        .connectionTimeMs(150)
        .connectReplyCode(220)
        .country("BE")
        .ipVersion(4)
        .startTlsOk(true)
        .connectOK(true)
        .crawlStarted(Instant.parse("2025-03-24T12:34:56Z"))
        .errorMessage(null)
        .software("Postfix")
        .softwareVersion("3.5.8")
        .startTlsReplyCode(400)
        .supportedExtensions(Set.of("PIPELINING", "8BITMIME"))
        .build();
  }


  public SmtpHost smtpHost1() {
    return SmtpHost.builder()
        .fromMx(true)
        .hostName("mail1.example.com")
        .priority(10)
        .conversations(List.of(smtpConversation1()))
        .build();
  }

  public SmtpHost smtpHost2() {
    return SmtpHost.builder()
        .fromMx(true)
        .hostName("mail2.example.com")
        .priority(10)
        .conversations(List.of(smtpConversation2()))
        .build();
  }

  public SmtpHost smtpHost3() {
    return SmtpHost.builder()
        .fromMx(true)
        .hostName("mail4.example.com")
        .priority(110)
        .conversations(List.of(smtpConversation3()))
        .build();
  }

  public SmtpHost smtpHost4() {
    return SmtpHost.builder()
        .fromMx(true)
        .hostName("mail4.example.com")
        .priority(110)
        .conversations(List.of(smtpConversation4()))
        .build();
  }


  public SmtpVisit smtpVisitWithNullValues() {
    return SmtpVisit.builder()
        .visitId(null)
        .hosts(null)
        .domainName(null)
        .crawlStarted(null)
        .crawlStatus(null)
        .build();
  }

  public SmtpVisit smtpVisitWithNullValues2() {
    return SmtpVisit.builder()
        .visitId(null)
        .hosts(List.of(smtpHostWithNullValues1()))
        .domainName(null)
        .crawlStarted(null)
        .crawlStatus(null)
        .build();
  }

  public SmtpVisit smtpVisitWithNullValues3() {
    return SmtpVisit.builder()
        .visitId(null)
        .hosts(List.of(smtpHostWithNullValues2()))
        .domainName(null)
        .crawlStarted(null)
        .crawlStatus(null)
        .build();
  }

  public SmtpHost smtpHostWithNullValues1() {
    return SmtpHost.builder()
        .fromMx(false)
        .hostName(null)
        .priority(0)
        .conversations(List.of())
        .build();
  }

  public SmtpHost smtpHostWithNullValues2() {
    return SmtpHost.builder()
        .fromMx(false)
        .hostName(null)
        .priority(0)
        .conversations(List.of(smtpConversationWithNullValues()))
        .build();
  }


  public SmtpConversation smtpConversationWithNullValues() {
    return SmtpConversation.builder()
        .asn(null)
        .ip(null)
        .asnOrganisation(null)
        .banner(null)
        .connectReplyCode(0)
        .country(null)
        .ipVersion(0)
        .startTlsOk(false)
        .connectOK(false)
        .crawlStarted(null)
        .errorMessage(null)
        .error(null)
        .software(null)
        .softwareVersion(null)
        .startTlsReplyCode(0)
        .supportedExtensions(null)
        .connectionTimeMs(0)
        .build();
  }

  public DnsCrawlResult dnsCrawlResultWithMultipleResponses1(String domain, String visitId) {
    ResponseGeoIp geoIp1 = new ResponseGeoIp(Pair.of(12345L, "ISP Belgium"), "BE", 4, "192.168.1.1");
    ResponseGeoIp geoIp2 = new ResponseGeoIp(Pair.of(67890L, "ISP France"), "FR", 4, "192.168.1.1");

    Response response1 = Response.builder()
        .recordData("192.168.1.1")
        .ttl(3600L)
        .responseGeoIps(List.of(geoIp1, geoIp2))
        .build();

    ResponseGeoIp geoIp3 = new ResponseGeoIp(Pair.of(54321L, "ISP Netherlands"), "NL", 4, "192.168.1.2");
    ResponseGeoIp geoIp4 = new ResponseGeoIp(Pair.of(98765L, "ISP Germany"), "DE", 4, "192.168.1.2");

    Response response2 = Response.builder()
        .recordData("192.168.1.2")
        .ttl(3600L)
        .responseGeoIps(List.of(geoIp3, geoIp4))
        .build();

    Request request = Request.builder()
        .domainName(domain)
        .prefix("www")
        .recordType(RecordType.A)
        .rcode(0)
        .crawlStarted(Instant.parse("2025-03-28T12:00:00Z"))
        .ok(true)
        .responses(List.of(response1, response2))
        .build();

    return new DnsCrawlResult(List.of(request), be.dnsbelgium.mercator.dns.dto.CrawlStatus.OK, domain, Instant.parse("2025-03-28T12:00:00Z"), null, visitId);
  }

  public DnsCrawlResult dnsCrawlResultWithMultipleResponses2(String domain, String visitId) {
    ResponseGeoIp geoIp1 = new ResponseGeoIp(Pair.of(1245L, "ISP Netherlands"), "BE", 4, "192.1681.1");
    ResponseGeoIp geoIp2 = new ResponseGeoIp(Pair.of(6780L, "ISP Belgium"), "FR", 4, "192.168.11");

    Response response1 = Response.builder()
        .recordData("192.1681.1")
        .ttl(3600L)
        .responseGeoIps(List.of(geoIp1, geoIp2))
        .build();

    ResponseGeoIp geoIp3 = new ResponseGeoIp(Pair.of(5321L, "ISP Germany"), "NL", 4, "192.1681.2");
    ResponseGeoIp geoIp4 = new ResponseGeoIp(Pair.of(9865L, "ISP Belgium"), "DE", 4, "192.1681.2");

    Response response2 = Response.builder()
        .recordData("192.1681.2")
        .ttl(3600L)
        .responseGeoIps(List.of(geoIp3, geoIp4))
        .build();

    Request request = Request.builder()
        .domainName(domain)
        .prefix("www")
        .recordType(RecordType.A)
        .rcode(0)
        .crawlStarted(Instant.parse("2025-03-28T12:00:00Z"))
        .ok(true)
        .responses(List.of(response1, response2))
        .build();

    return new DnsCrawlResult(List.of(request), be.dnsbelgium.mercator.dns.dto.CrawlStatus.OK, domain, Instant.parse("2025-03-28T12:00:00Z"), null, visitId);
  }


  public DnsCrawlResult dnsCrawlResultWithNullValues() {
    return new DnsCrawlResult(null, null, null, null, null, null);
  }

  public DnsCrawlResult dnsCrawlResultWithNullRequest() {
    Request request = Request.builder()
        .domainName(null)
        .prefix(null)
        .recordType(null)
        .rcode(null)
        .crawlStarted(null)
        .ok(false)
        .responses(List.of())
        .build();

    return new DnsCrawlResult(List.of(request), null, null, null, null, null);
  }

  public DnsCrawlResult dnsCrawlResultWithNullResponse() {
    Response response1 = Response.builder()
        .recordData(null)
        .ttl(null)
        .responseGeoIps(null)
        .build();

    Request request = Request.builder()
        .domainName(null)
        .prefix(null)
        .recordType(null)
        .rcode(null)
        .crawlStarted(null)
        .ok(false)
        .responses(List.of(response1))
        .build();

    return new DnsCrawlResult(List.of(request), null, null, null, null, null);
  }

  public DnsCrawlResult dnsCrawlResultWithNullGeoIp() {
    ResponseGeoIp geoIp1 = new ResponseGeoIp(Pair.of(null, null), null, 0, null);

    Response response1 = Response.builder()
        .recordData(null)
        .ttl(null)
        .responseGeoIps(List.of(geoIp1))
        .build();


    Request request = Request.builder()
        .domainName(null)
        .prefix(null)
        .recordType(null)
        .rcode(null)
        .crawlStarted(null)
        .ok(false)
        .responses(List.of(response1))
        .build();

    return new DnsCrawlResult(List.of(request), null, null, null, null, null);
  }


}