package be.dnsbelgium.mercator.common;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;

@Component("dateTimeUtil")
public class DateTimeFormatterUtil {

    @Value("${web.ui.datetime.pattern}")
    private String pattern;

    @Value("${web.ui.timezone}")
    private String zoneId;

    public String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(zoneId));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(zonedDateTime);
    }

}
