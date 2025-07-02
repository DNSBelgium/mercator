
package be.dnsbelgium.mercator.web.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.web.domain.Page;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;


import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static be.dnsbelgium.mercator.web.wappalyzer.jappalyzer.MetricName.TIMER_JAPPALYZER_TECHNOLOGY_MATCH_APPLICABLE_TO;

public class Technology {

    @Getter
    private final String name;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String iconName;

    @Getter
    @Setter
    private String website;

    @Getter
    private String cpe;

    @Getter
    @Setter
    private boolean saas;

    @Getter
    private final List<String> pricing = new LinkedList<>();

    @Getter
    private final List<Category> categories = new LinkedList<>();

    @Getter
    private final List<String> implies = new LinkedList<>();

    private final List<PatternWithVersion> htmlTemplates = new LinkedList<>();
    private final List<DomPattern> domTemplates = new LinkedList<>();
    @Getter
    private final List<PatternWithVersion> scriptSrc = new LinkedList<>();
    private final Map<String, List<PatternWithVersion>> headerTemplates = new HashMap<>();
    private final Map<String, List<PatternWithVersion>> cookieTemplates = new HashMap<>();
    private final Map<String, List<PatternWithVersion>> metaTemplates = new HashMap<>();

    private final Timer applicableToTimer;

    public Technology(String name, MeterRegistry meterRegistry) {
      this.name = name;
      this.applicableToTimer = Timer.builder(TIMER_JAPPALYZER_TECHNOLOGY_MATCH_APPLICABLE_TO)
                .publishPercentiles(0.5, 0.80, 0.95, 0.99)
                .description("Time needed to check if a Technology is applicable to a page")
                .register(meterRegistry);
    }

    public void setCPE(String cpe) {
        this.cpe = cpe;
    }

    public void addPricing(String pricing) {
        this.pricing.add(pricing);
    }

    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public void addImplies(String imply) {
        this.implies.add(imply);
    }

    public void addHtmlTemplate(String template) {
        this.htmlTemplates.add(new PatternWithVersion(template));
    }

    public List<PatternWithVersion> getMetaTemplates(String name) {
        List<PatternWithVersion> patterns = this.metaTemplates.get(name);
        if (patterns == null) {
            return Collections.emptyList();
        }
        return patterns;
    }

    public void addMetaTemplate(String name, String pattern) {
        this.metaTemplates.putIfAbsent(name, new LinkedList<>());
        this.metaTemplates.get(name).add(new PatternWithVersion(pattern));
    }

    public void addCookieTemplate(String cookie, String cookiePattern) {
        this.cookieTemplates.putIfAbsent(cookie, new LinkedList<>());
        this.cookieTemplates.get(cookie).add(new PatternWithVersion(cookiePattern));
    }

    public List<PatternWithVersion> getHeaderTemplates(String headerKey) {
        return headerTemplates.get(headerKey.toLowerCase());
    }

    public void addHeaderTemplate(String headerName, String template) {
        this.headerTemplates.putIfAbsent(headerName.toLowerCase(), new LinkedList<>());
        this.headerTemplates.get(headerName.toLowerCase()).add(new PatternWithVersion(template));
    }

    public List<DomPattern> getDomPatterns() {
        return domTemplates;
    }

    public void addDomPattern(DomPattern template) {
        this.domTemplates.add(template);
    }

    public void addScriptSrc(String scriptSrc) {
        this.scriptSrc.add(new PatternWithVersion(scriptSrc));
    }

    public TechnologyMatch applicableTo(JappalyzerPage jappalyzerPage) {
        if (jappalyzerPage.shouldAbort()) {
            // already spent too long analysing this page => give up
            return abort();
        }
        long start = System.currentTimeMillis();
        TechnologyMatch technologyMatch = match(jappalyzerPage);
        long millis =  System.currentTimeMillis() - start;
        jappalyzerPage.record(this.name, millis);

        return technologyMatch;
    }

    public TechnologyMatch applicableTo(Page page) {
        JappalyzerPage jappalyzerPage = new JappalyzerPage(page, Instant.now(), Duration.ofSeconds(10));
        return applicableToTimer.record(() -> match(jappalyzerPage));
    }

    private TechnologyMatch match(JappalyzerPage jappalyzerPage) {
        long startTimestamp = System.currentTimeMillis();
        Page page = jappalyzerPage.getPage();

        if (!page.getHeaders().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.headerTemplates, page.getHeaders());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.HEADER, true, duration);
        }
        if (jappalyzerPage.shouldAbort()) {
            return abort();
        }

        if (!page.getCookies().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.cookieTemplates, page.getCookies());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.COOKIE, true, duration);
        }
        if (jappalyzerPage.shouldAbort()) {
            return abort();
        }

        if (!page.getMetaMap().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.metaTemplates, page.getMetaMap());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.META, true, duration);
        }

        for (DomPattern domTemplate : this.domTemplates) {
            if (jappalyzerPage.shouldAbort()) {
                return abort();
            }
            if (domTemplate.applicableToDocument(jappalyzerPage)) {
                long duration = System.currentTimeMillis() - startTimestamp;
                return new TechnologyMatch(this, TechnologyMatch.DOM, duration);
            }
        }

        for (PatternWithVersion scriptSrcPattern : this.scriptSrc) {
            if (jappalyzerPage.shouldAbort()) {
                return abort();
            }
            PatternMatch match = getTechnologyStringListMatch(page.getScriptSources(), scriptSrcPattern);
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.SCRIPT, true, duration);
        }

        for (PatternWithVersion htmlTemplate : this.htmlTemplates) {
            if (jappalyzerPage.shouldAbort()) {
                return abort();
            }
            PatternMatch match = getTechnologyStringListMatch(page.getResponseBody().lines().toList(), htmlTemplate);
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched()) {
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.HTML, true, duration);
            }
        }

        long duration = System.currentTimeMillis() - startTimestamp;
        return TechnologyMatch.notMatched(this, duration);
    }

    private TechnologyMatch abort() {
        return TechnologyMatch.notMatched(this, 0);
    }

    private PatternMatch getTechnologyStringListMatch(List<String> lines, PatternWithVersion pattern) {
        for (String line : lines) {
            if (line.length() > 500) {
                line = line.substring(0, 500);
            }
            PatternMatch match = pattern.match(line);
            if (match.isMatched()) {
                return match;
            }
        }
        return new PatternMatch(false, "");
    }

    private PatternMatch getTechnologyMapMatch(Map<String, List<PatternWithVersion>> templates,
                                               Map<String, List<String>> page) {
        for (String header : templates.keySet()) {
            List<PatternWithVersion> patterns = templates.get(header);
            for (PatternWithVersion pattern : patterns) {
                if (pattern.toString().isEmpty() && page.containsKey(header)) {
                    return new PatternMatch(true, "");
                } else {
                    List<String> headerValues = page.get(header);
                    if (headerValues != null && !headerValues.isEmpty()) {
                        for (String value : headerValues) {
                            PatternMatch match = pattern.match(value);
                            if (match.isMatched()) {
                                return match;
                            }
                        }
                    }
                }
            }
        }
        return new PatternMatch(false, "");
    }

    @Override
    public String toString() {
        return "Technology{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", iconName='" + iconName + '\'' +
                ", website='" + website + '\'' +
                '}';
    }
}
