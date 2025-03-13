package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import java.util.Objects;

public class TechnologyMatch {

    public static final String HEADER = "header";
    public static final String COOKIE = "cookie";
    public static final String META = "meta";
    public static final String DOM = "dom";
    public static final String SCRIPT = "script";
    public static final String HTML = "html";
    public static final String IMPLIED = "implied";

    private final Technology technology;
    private final long duration;
    private final String reason;
    private final boolean matched;
    private final String version;

    public TechnologyMatch(Technology technology, String reason) {
        this(technology, reason, 0L);
    }

    public TechnologyMatch(Technology technology, String reason, long duration) {
        this(technology, "", reason, true, duration);
    }

    public TechnologyMatch(Technology technology, String version, String reason, boolean matched, long duration) {
        this.technology = technology;
        if (version == null) {
            this.version = "";
        } else {
            this.version = version;
        }
        this.matched = matched;
        this.duration = duration;
        this.reason = reason;
    }

    public static TechnologyMatch notMatched(Technology technology, long duration) {
        return new TechnologyMatch(technology, "", "", false, duration);
    }

    public boolean isMatched() {
        return matched;
    }

    public Technology getTechnology() {
        return technology;
    }

    public String getVersion() {
        return version;
    }

    public long getDuration() {
        return duration;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TechnologyMatch match = (TechnologyMatch) o;
        return matched == match.matched && Objects.equals(technology, match.technology)
                && Objects.equals(reason, match.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technology, reason, matched);
    }

    @Override
    public String toString() {
        if (matched) {
            return "TechnologyMatch{" +
                    "technology=" + technology.getName() +
                    ", version='" + version + "'" +
                    ", reason=" + reason +
                    ", duration=" + duration + "ms" +
                    ", categories=" + technology.getCategories() +
                    '}';
        } else {
            return "TechnologyMatch{" +
                    "technology=" + technology.getName() +
                    ", notMatched}";
        }
    }
}
