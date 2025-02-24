package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

public class PatternMatch {

    private final boolean matched;
    private final String version;

    public PatternMatch(boolean matched, String version) {
        this.matched = matched;
        this.version = version;
    }

    public boolean isMatched() {
        return matched;
    }

    public String getVersion() {
        return version;
    }
}
