package be.dnsbelgium.mercator.dns;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.List;

@ActiveProfiles({"local", "test"})
@SpringBootTest
public class FooTest {
    @Test
    void foo() throws IOException {
        final Name full = Name.concatenate(Name.fromString("dnsbelgium.be"), Name.root);
        final Resolver res = newResolver();
        final Record question = Record.newRecord(full, Type.SOA, DClass.IN);
        final Message query = Message.newQuery(question);
        final Message response = res.send(query);
        final RRset[] answer = response.getSectionRRsets(Section.ANSWER).toArray(new RRset[0]);
        for (RRset record : answer) {
            List<RRSIGRecord> rrsigRecords = record.sigs();
            for (RRSIGRecord rrsig : rrsigRecords) {
                System.out.println(" --- ");
                System.out.println(rrsig.rdataToString()); // <-- (SOA) RRSIG data to add to DB.
                System.out.println(" --- ");
            }
        }
    }
    private static Resolver newResolver() {
        final Resolver res = new ExtendedResolver();
        res.setEDNS(0, 0, ExtendedFlags.DO, (List<EDNSOption>) null);
        res.setIgnoreTruncation(false);
        return res;
    }
}