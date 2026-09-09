# End-to-end black-box parity: Spring Batch vs new pipeline

Two **independent JVMs** ran the packaged application (`target/mercator-2.0.0-SNAPSHOT.jar`)
end-to-end, each performing its **own real crawl** of `dnsbelgium.be` (visitId `v1`):

| Run                 | How it was started                                                                           | PID   | Output (Parquet)                                                  |
|---------------------|----------------------------------------------------------------------------------------------|-------|-------------------------------------------------------------------|
| Legacy Spring Batch | `--spring.profiles.active=batch` (→ `mercator.batch.enabled=true` → `JobRunner`, self-exits) | 79477 | `/tmp/mercator-e2e/batch/data/{dns,tls,web,web_response_body}`    |
| New pipeline        | `--mercator.batch.enabled=false` (→ `PipelineRunner` on `ApplicationReadyEvent`)             | 79535 | `/tmp/mercator-e2e/pipeline/data/{dns,tls,web,web_response_body}` |

No test fixtures, no shared objects — each JVM crawled independently and wrote Parquet via the
module `repository.storeResults(...)`. Parquet was kept (not deleted). Comparison done with the
DuckDB CLI (`compare.sql`).

## Results

**1. Schema parity** — identical for every dataset (0 differences both directions):
`dns`, `web`, `web_response_body`, `tls`.

**2. Raw full-row diff, excluding only top-level timestamps** (`crawl_started`,
`crawl_finished`, `year`, `month`): `dns` 1/1, `web` 1/1, `tls` 1/1, `web_response_body` 0/0.
→ The single row per module differs **only** because of *nested* volatile values (per-request
DNS timestamps + TTL, per-page web timestamps, TLS scan timestamps + `millis_*` durations).
This is exactly the timing noise expected between two independent real crawls.

**3. Stable semantic content** (excluding all timestamps / TTL / durations):

| Check | batch-only | pipe-only |
|-------|-----------|-----------|
| dns_request (prefix, record_type, rcode, ok, num_of_responses) | 0 | 0 |
| dns_response (record_data) | 0 | 0 |
| web_response_body (url, final_url, response_body) | 0 | 0 |
| tls_visits (host, cert fingerprints, version/cipher support, …) | 0 | 0 |
| web_page_visits (url, final_url, status, content_length, tech, …) | 3 | 3 → **0/0 after sorting `detected_technologies`** |

The only content difference was the **order** of the `detected_technologies` array (same set,
different order due to concurrent technology detection within a single crawl). Order-normalizing
the list yields 0/0.

## Conclusion

Running the real application twice — Spring Batch vs the new pipeline — on the same input yields
**structurally identical Parquet and identical semantic content**. All remaining differences are
volatile data (timestamps, TTLs, scan durations, and list ordering), i.e. the "small changes"
that must be ignored. This is a genuine two-JVM, twice-crawled comparison.

## Reproduce

- Run both JVMs: `bash /tmp/mercator-e2e-run.sh`
- Compare: `duckdb -init /dev/null < /tmp/mercator-e2e/compare.sql`
- Web drill-down: `duckdb -init /dev/null < /tmp/mercator-e2e/web_drill.sql`

