package be.dnsbelgium.mercator.integration;

import be.dnsbelgium.mercator.integration.ParquetDatasetComparator.ColumnInfo;
import be.dnsbelgium.mercator.integration.ParquetDatasetComparator.ComparisonResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates {@link ComparisonResult}s and renders a Markdown report summarizing whether the
 * new pipeline output is identical to the legacy Spring Batch output, highlighting any
 * differences found. The report is written even when the comparison fails, so a failing run
 * still yields a human-readable diff.
 */
public class ParityReport {

    private final List<ComparisonResult> results = new ArrayList<>();

    public void add(ComparisonResult result) {
        results.add(result);
    }

    public boolean allIdentical() {
        return results.stream().allMatch(ComparisonResult::identical);
    }

    public String render() {
        StringBuilder md = new StringBuilder();
        md.append("# Engine parity report\n\n");
        md.append("Comparison of the **new pipeline** output vs the **legacy Spring Batch** output.\n\n");
        md.append("Generated: ")
                .append(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")))
                .append("\n\n");

        md.append("## Summary\n\n");
        md.append("| Dataset | Schema match | Old rows | New rows | Only in old | Only in new | Verdict |\n");
        md.append("|---------|--------------|----------|----------|-------------|-------------|---------|\n");
        for (ComparisonResult r : results) {
            md.append("| ").append(r.label())
                    .append(" | ").append(r.schemaMatches() ? "yes" : "**NO**")
                    .append(" | ").append(r.oldRowCount())
                    .append(" | ").append(r.newRowCount())
                    .append(" | ").append(r.onlyInOld().size())
                    .append(" | ").append(r.onlyInNew().size())
                    .append(" | ").append(r.identical() ? "IDENTICAL" : "**DIFFERENCES**")
                    .append(" |\n");
        }
        md.append("\n");

        for (ComparisonResult r : results) {
            md.append("## ").append(r.label()).append("\n\n");
            md.append("Ignored (volatile) columns: ")
                    .append(r.ignoredColumns().isEmpty() ? "_none_" : "`" + String.join("`, `", r.ignoredColumns()) + "`")
                    .append("\n\n");

            if (r.identical()) {
                md.append("Output is **identical** (ignoring volatile columns).\n\n");
                continue;
            }

            if (!r.schemaMatches()) {
                md.append("### Schema mismatch\n\n");
                md.append("Old schema:\n\n").append(renderSchema(r.oldSchema()));
                md.append("New schema:\n\n").append(renderSchema(r.newSchema()));
            }
            appendSamples(md, "Rows only in OLD output", r.onlyInOld());
            appendSamples(md, "Rows only in NEW output", r.onlyInNew());
        }

        return md.toString();
    }

    public void write(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, render());
    }

    private static String renderSchema(List<ColumnInfo> schema) {
        StringBuilder sb = new StringBuilder("| Column | Type |\n|--------|------|\n");
        for (ColumnInfo c : schema) {
            sb.append("| ").append(c.name()).append(" | `").append(c.type()).append("` |\n");
        }
        return sb.append("\n").toString();
    }

    private static void appendSamples(StringBuilder md, String title, List<String> rows) {
        if (rows.isEmpty()) {
            return;
        }
        md.append("### ").append(title).append(" (showing ").append(rows.size()).append(")\n\n");
        md.append("```json\n");
        rows.forEach(r -> md.append(r).append("\n"));
        md.append("```\n\n");
    }
}

