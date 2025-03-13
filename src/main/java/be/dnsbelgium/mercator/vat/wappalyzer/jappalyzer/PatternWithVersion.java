
package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternWithVersion {

    private final Pattern pattern;
    private boolean hasVersion = false;

    public PatternWithVersion(String regexp) {
        String[] splittedRegexp = regexp.split("\\\\;");
        pattern = Pattern.compile(splittedRegexp[0]);
        if (splittedRegexp.length > 1) {
            hasVersion = true;
        }
    }

    public PatternMatch match(String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            if (hasVersion && matcher.groupCount() > 0) {
                String version = matcher.group(1);
                return new PatternMatch(true, version);
            } else {
                return new PatternMatch(true, "");
            }
        } else {
            return new PatternMatch(false, "");
        }
    }

    public String getPattern() {
        return pattern.toString();
    }

    @Override
    public String toString() {
        return "PatternWithVersion{" +
                "pattern=" + pattern +
                ", hasVersion=" + hasVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PatternWithVersion that = (PatternWithVersion) o;
        return hasVersion == that.hasVersion && Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, hasVersion);
    }
}
