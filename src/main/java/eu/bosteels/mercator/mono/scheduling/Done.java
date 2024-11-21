package eu.bosteels.mercator.mono.scheduling;

import java.time.Instant;

public record Done(String visitId, String domainName, Instant done) {



}
