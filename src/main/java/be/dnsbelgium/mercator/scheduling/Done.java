package be.dnsbelgium.mercator.scheduling;

import java.time.Instant;

public record Done(String visitId, String domainName, Instant done) {



}
