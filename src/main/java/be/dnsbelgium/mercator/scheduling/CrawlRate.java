package be.dnsbelgium.mercator.scheduling;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record CrawlRate(
        ZonedDateTime dateTime,
        double perHour,
        double perMinute,
        double perSecond
) {

    @Override
    public String toString() {
        return "CrawlRate{" +
                "dateTime=" + dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) +
                ", perHour="   + String.format(Locale.ENGLISH, "%,.0f", perHour ) +
                ", perMinute=" + String.format(Locale.ENGLISH, "%,.2f", perMinute) +
                ", perSecond=" + String.format(Locale.ENGLISH, "%,.2f", perSecond) +
                '}';
    }
}
