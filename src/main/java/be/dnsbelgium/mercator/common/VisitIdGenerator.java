package be.dnsbelgium.mercator.common;

import com.github.f4b6a3.ulid.UlidCreator;

public class VisitIdGenerator {

    public static String generate() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
