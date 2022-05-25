package be.dnsbelgium.mercator.dns;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class FooTest {

    private Lookup initLookup(Name name, int value) throws UnknownHostException {
        Lookup lookup = new Lookup(name, value);
        lookup.setResolver(newSimpleResolver());
        return lookup;
    }

    @Test
    void bar() throws IOException {
        final Name full = Name.concatenate(Name.fromString("dnsbelgium.be"), Name.root);
        Lookup lookup = initLookup(full, Type.value("RRSIG"));
        lookup.run();
        Record[] answers = lookup.getAnswers();
        for (Record answer : answers) {
            System.out.println(" --- ");
            System.out.println(answer.toString());
            System.out.println(" --- ");
        }
    }

    @Test
    void foo() throws IOException {
        final Name full = Name.concatenate(Name.fromString("dnsbelgium.be"), Name.root);
        final Resolver res = newSimpleResolver();
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
    private static Resolver newExtendedResolver() throws UnknownHostException {
        final Resolver res = new ExtendedResolver(new String[]{"8.8.8.8"});
        res.setEDNS(0, 0, ExtendedFlags.DO, (List<EDNSOption>) null);
        res.setIgnoreTruncation(false);
        return res;
    }

    private static Resolver newSimpleResolver() throws UnknownHostException {
        final Resolver res = new SimpleResolver("8.8.8.8");
        res.setEDNS(0, 0, ExtendedFlags.DO, (List<EDNSOption>) null);
        res.setIgnoreTruncation(false);
        return res;
    }
}