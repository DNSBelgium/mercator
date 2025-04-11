package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.smtp.dto.Error;
import be.dnsbelgium.mercator.smtp.dto.CrawlStatus;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.dto.SmtpHost;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import be.dnsbelgium.mercator.test.TestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class SmtpTestUtils {

    private static final Logger logger = getLogger(SmtpTestUtils.class);

    public static InetAddress ip(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.info("e = {}", e.getMessage());
        }
    }

    public static MXRecord mxRecord(String owner, int ttl, String target) {
        return new MXRecord(fromString(owner), DClass.IN, ttl, 10, fromString(target));
    }

    private static Name fromString(String name) {
        try {
            if (name.endsWith("."))
                return Name.fromString(name);
            else
                return Name.fromString(name + ".");
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static SmtpVisit visit() {
        SmtpHost host1 = host(RandomStringUtils.secure().nextAscii(8));
        SmtpHost host2 = host(RandomStringUtils.secure().nextAscii(8));
        SmtpVisit visit = SmtpVisit.builder()
          .visitId(VisitIdGenerator.generate())
          //.hosts(List.of(host1, host2))
          .domainName("example.com")
          .crawlStatus(CrawlStatus.OK)
          .timestamp(TestUtils.now())
          .numConversations(2)
          .build();
        visit.add(host1);
        visit.add(host2);
        return visit;
    }

    public static SmtpHost host(String id) {
        return SmtpHost.builder()
            .id(id)
            .hostName("smtp1.example.org")
            .conversation(conversation())
            .fromMx(true)
            .priority(10)
            .build();
    }

    public static SmtpConversation conversation() {
        return SmtpConversation.builder()
            .ip("127.0.0.1")
            .asn(14506L)
            .asnOrganisation("Acme Corp.")
            .country("BE")
            .banner("Welcome to Acme SMTP server")
            .connectOK(true)
            .connectReplyCode(220)
            .ipVersion(4)
            .connectionTimeMs(123)
            .startTlsOk(true)
            .startTlsReplyCode(230)
            .software("ACME SMTP")
            .softwareVersion("0.never")
            .timestamp(TestUtils.now())
            .supportedExtensions(Set.of("SMTPUTF8", "SIZE 157286400"))
            .errorMessage("Connection timed out")
            .error(Error.TIME_OUT)
            .build();
    }

    public static SmtpVisit smtpVisitWithBinaryData() {
        var visitId = SmtpVisit.generateVisitId();
        var conversation = SmtpConversation.builder()
            .ip("1.2.3.4")
            .connectReplyCode(220)
            .ipVersion(4)
            .banner("my binary \u0000 banner")
            .connectionTimeMs(123)
            .startTlsOk(false)
            .country("Jamaica \u0000")
            .asnOrganisation("Happy \u0000 Green grass")
            .asn(654L)
            .timestamp(TestUtils.now())
            .build();
        SmtpHost host = SmtpHost.builder()
            .id(RandomStringUtils.secure().nextAlphanumeric(10))
            .hostName("smtp1.example.com")
            .conversation(conversation)
            .build();
        return SmtpVisit.builder()
            .visitId(visitId)
            .domainName("dnsbelgium.be")
            .timestamp(TestUtils.now())
            .hosts(List.of(host))
            .build();
    }


}
