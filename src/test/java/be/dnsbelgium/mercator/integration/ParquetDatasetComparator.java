package be.dnsbelgium.mercator.integration;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.duckDbClient;

/**
 * Compares two Parquet datasets (produced by the legacy Spring Batch writer and by the new
 * pipeline writer) and reports whether they are structurally and content-wise identical.
 *
 * <p>The comparison:
 * <ol>
 *   <li>compares the column <b>schema</b> (name + type, in order) via {@code DESCRIBE};</li>
 *   <li>compares the <b>set of rows</b>, ignoring a configurable set of volatile top-level
 *       columns (e.g. {@code crawl_started}, {@code year}, {@code month}). Each row is turned
 *       into a JSON string with DuckDB's {@code to_json}, so nested {@code STRUCT}/{@code LIST}/
 *       {@code MAP} columns are compared structurally without fragile set operations.</li>
 * </ol>
 *
 * <p>Rows are compared as a multiset, so ordering and the number of Parquet files (the new
 * writer produces many small files, the old one a few large ones) are irrelevant.
 */
public class ParquetDatasetComparator {

    private static final int MAX_SAMPLES = 20;

    private final JdbcClient client = duckDbClient();

    public record ColumnInfo(String name, String type) {
    }

    public record ComparisonResult(
            String label,
            boolean schemaMatches,
            List<ColumnInfo> oldSchema,
            List<ColumnInfo> newSchema,
            long oldRowCount,
            long newRowCount,
            List<String> ignoredColumns,
            List<String> onlyInOld,
            List<String> onlyInNew) {

        public boolean identical() {
            return schemaMatches && onlyInOld.isEmpty() && onlyInNew.isEmpty();
        }
    }

    /**
     * @param label         human-readable name of the dataset (e.g. "web", "dns", "tls")
     * @param oldBase       base directory of the Parquet written by the legacy writer
     * @param newBase       base directory of the Parquet written by the new writer
     * @param ignoreColumns top-level column names to ignore (volatile data such as timestamps)
     */
    public ComparisonResult compare(String label, Path oldBase, Path newBase, List<String> ignoreColumns) {
        List<ColumnInfo> oldSchema = describe(oldBase);
        List<ColumnInfo> newSchema = describe(newBase);
        boolean schemaMatches = oldSchema.equals(newSchema);

        // Only ignore columns that actually exist (year/month may be encoded in the path).
        List<String> existing = oldSchema.stream().map(ColumnInfo::name).toList();
        List<String> effectiveIgnore = ignoreColumns.stream().filter(existing::contains).toList();

        List<String> oldRows = rowsAsJson(oldBase, effectiveIgnore);
        List<String> newRows = rowsAsJson(newBase, effectiveIgnore);

        Map<String, Integer> counts = new LinkedHashMap<>();
        oldRows.forEach(r -> counts.merge(r, 1, Integer::sum));
        newRows.forEach(r -> counts.merge(r, -1, Integer::sum));

        List<String> onlyInOld = new ArrayList<>();
        List<String> onlyInNew = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            int diff = e.getValue();
            for (int i = 0; i < diff && onlyInOld.size() < MAX_SAMPLES; i++) {
                onlyInOld.add(e.getKey());
            }
            for (int i = 0; i < -diff && onlyInNew.size() < MAX_SAMPLES; i++) {
                onlyInNew.add(e.getKey());
            }
        }

        return new ComparisonResult(label, schemaMatches, oldSchema, newSchema,
                oldRows.size(), newRows.size(), effectiveIgnore, onlyInOld, onlyInNew);
    }

    private List<ColumnInfo> describe(Path base) {
        String sql = "DESCRIBE SELECT * FROM read_parquet('" + glob(base) + "')";
        List<Map<String, Object>> rows = client.sql(sql).query().listOfRows();
        List<ColumnInfo> columns = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            columns.add(new ColumnInfo(
                    String.valueOf(row.get("column_name")),
                    String.valueOf(row.get("column_type"))));
        }
        return columns;
    }

    private List<String> rowsAsJson(Path base, List<String> ignoreColumns) {
        String projection = ignoreColumns.isEmpty()
                ? "*"
                : "* EXCLUDE (" + String.join(", ", ignoreColumns) + ")";
        String sql = """
                with data as (select %s from read_parquet('%s'))
                select to_json(t) from data as t
                """.formatted(projection, glob(base));
        return client.sql(sql).query(String.class).list();
    }

    private static String glob(Path base) {
        return base.toAbsolutePath() + "/**/*.parquet";
    }
}

