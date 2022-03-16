package be.dnsbelgium.mercator.smtp;

import org.slf4j.Logger;
import org.xbill.DNS.DClass;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;

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


}
