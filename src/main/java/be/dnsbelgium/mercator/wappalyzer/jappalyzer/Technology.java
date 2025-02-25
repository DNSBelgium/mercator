
package be.dnsbelgium.mercator.wappalyzer.jappalyzer;

import java.util.*;

public class Technology {

    private final String name;
    private String description;
    private String iconName;
    private String website;
    private String cpe;
    private boolean saas;
    private final List<String> pricing = new LinkedList<>();
    private final List<Category> categories = new LinkedList<>();
    private final List<String> implies = new LinkedList<>();

    private final List<PatternWithVersion> htmlTemplates = new LinkedList<>();
    private final List<DomPattern> domTemplates = new LinkedList<>();
    private final List<PatternWithVersion> scriptSrc = new LinkedList<>();
    private final Map<String, List<PatternWithVersion>> headerTemplates = new HashMap<>();
    private final Map<String, List<PatternWithVersion>> cookieTemplates = new HashMap<>();
    private final Map<String, List<PatternWithVersion>> metaTemplates = new HashMap<>();

    public Technology(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCPE() {
        return this.cpe;
    }

    public void setCPE(String cpe) {
        this.cpe = cpe;
    }

    public boolean isSaas() {
        return saas;
    }

    public void setSaas(boolean saas) {
        this.saas = saas;
    }

    public List<String> getPricing() {
        return pricing;
    }

    public void addPricing(String pricing) {
        this.pricing.add(pricing);
    }

    public List<Category> getCategories() {
        return this.categories;
    }

    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public List<String> getImplies() {
        return this.implies;
    }

    public void addImplies(String imply) {
        this.implies.add(imply);
    }

    public List<PatternWithVersion> getHtmlTemplates() {
        return htmlTemplates;
    }

    public void addHtmlTemplate(String template) {
        // Pattern pattern = Pattern.compile(prepareRegexp(template));
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

    public Map<String, List<PatternWithVersion>> getCookieTemplates() {
        return this.cookieTemplates;
    }

    public void addCookieTemplate(String cookie, String cookiePattern) {
        this.cookieTemplates.putIfAbsent(cookie, new LinkedList<>());
        this.cookieTemplates.get(cookie).add(new PatternWithVersion(cookiePattern));
    }

    public Map<String, List<PatternWithVersion>> getHeaderTemplates() {
        return headerTemplates;
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

    public List<PatternWithVersion> getScriptSrc() {
        return scriptSrc;
    }

    public void addScriptSrc(String scriptSrc) {
        this.scriptSrc.add(new PatternWithVersion(scriptSrc));
    }

    public TechnologyMatch applicableTo(String content) {
        return applicableTo(new PageResponse(content));
    }

    public TechnologyMatch applicableTo(PageResponse page) {
        long startTimestamp = System.currentTimeMillis();

        if (!page.getHeaders().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.headerTemplates, page.getHeaders());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.HEADER, true, duration);
        }

        if (!page.getCookies().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.cookieTemplates, page.getCookies());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.COOKIE, true, duration);
        }

        if (!page.getMetaMap().isEmpty()) {
            PatternMatch match = getTechnologyMapMatch(this.metaTemplates, page.getMetaMap());
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.META, true, duration);
        }

        for (DomPattern domTemplate : this.domTemplates) {
            if (domTemplate.applicableToDocument(page.getDocument())) {
                long duration = System.currentTimeMillis() - startTimestamp;
                return new TechnologyMatch(this, TechnologyMatch.DOM, duration);
            }
        }

        for (PatternWithVersion scriptSrcPattern : this.scriptSrc) {
            PatternMatch match = getTechnologyStringListMatch(page.getScriptSources(), scriptSrcPattern);
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.SCRIPT, true, duration);
        }

        for (PatternWithVersion htmlTemplate : this.htmlTemplates) {
            PatternMatch match = getTechnologyStringListMatch(page.getContentLines(), htmlTemplate);
            long duration = System.currentTimeMillis() - startTimestamp;
            if (match.isMatched())
                return new TechnologyMatch(this, match.getVersion(), TechnologyMatch.HTML, true, duration);
        }

        long duration = System.currentTimeMillis() - startTimestamp;
        return TechnologyMatch.notMatched(this, duration);
    }

    private PatternMatch getTechnologyStringListMatch(List<String> lines, PatternWithVersion pattern) {
        for (String line : lines) {
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
