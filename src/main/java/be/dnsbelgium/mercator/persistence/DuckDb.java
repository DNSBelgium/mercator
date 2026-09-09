package be.dnsbelgium.mercator.persistence;

/**
 * Small helpers for working with DuckDB SQL.
 */
public final class DuckDb {

    private DuckDb() {
    }

    /**
     * Identity function that returns {@code sql} unchanged.
     *
     * <p>Its only purpose is to defuse IntelliJ's automatic SQL language injection. IntelliJ
     * injects a generic SQL dialect into a string literal passed <em>directly</em> to
     * {@code JdbcClient.sql(...)}, which then flags perfectly valid <strong>DuckDB</strong>
     * syntax — e.g. reading a file directly in the {@code FROM} clause
     * ({@code from 'foo.parquet'}) — as {@code "<comma join expression> expected"}. Wrapping the
     * literal as {@code sql(nop("select ... from 'file'"))} means the literal is no longer the
     * direct argument of {@code sql(...)}, so no dialect is injected and the false positive
     * disappears, without changing runtime behavior.
     */
    public static String nop(String sql) {
        return sql;
    }
}

