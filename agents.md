# Intro

This repo contains a crawler for collecting data on how domain names are being used.
It consists of multiple independent modules that each process VisitRequests and produce parquet output files.
Each module has its own set of POJOs; these are saved as JSON files using Jackson and then rolled into Parquet files using DuckDB.
The modules are:
* dns: collects various DNS records 
* smtp: resolves MX records and attempts to connect to the SMTP server
* web: crawls the web page and collects HTML, metadata, links, etc
* tls: collects which TLS versions and ciphers are supported by the web server + certificate information

Currently, the project uses Spring Batch to manage the pipeline, but it is being refactored to use a
custom pipeline engine that uses virtual threads and a producer/consumer model.

## Main application:
- `MercatorApplication` is the single active `@SpringBootApplication` entry point.
- `PipelineApplication` (package `...pipeline`) holds the module-orchestration logic (`run()`,
  `resolveModules`, `QueueModuleRunner` wiring) but is currently disabled — its
  `@SpringBootApplication`/`main`/`@EventListener` are commented out.

## Build & Test

`mvn` is on the PATH (sdkman → Maven 3.9.12, JDK 25). **Always use `mvn` — never `./mvnw`.**

DuckDB's JNI needs `--enable-native-access=ALL-UNNAMED`; this is already wired into the
compiler, surefire and jar-manifest configs in `pom.xml`, so no extra flags are needed.

### Snyk is a real security gate — do NOT work around it
The build runs the Snyk plugin to detect dependency vulnerabilities (CVEs). A Snyk failure
means there **are** vulnerabilities: the correct response is to **fix them** — bump the
offending dependency to a patched version. **Never** disable, remove, or profile-gate the Snyk
plugin, and never assume "CI will catch it".

`-Dsnyk.skip` exists **only** to speed up the **local test loop**: it skips the scan, it does
**not** fix or validate anything. Use it while iterating on code/tests, but run the full build
(with Snyk) before calling a change done.

#### Capturing the FULL Snyk report (learned the hard way)
When Snyk fails, the useful part is the **list of vulnerable paths** it prints — but that report
is long and the terminal tool routinely **truncates or drops** it, so `mvn -q test` leaves you
guessing which dependency to bump. Do **not** reach for `| tee`, `> file`, or `| grep` (those
break the pipe/approval rules above and still race with buffering). Instead use Maven's **native
`-l <file>` log option**, which writes *all* output to a file with no shell redirection:

```
mvn test -l snyk-build.log
```

Notes:
- Drop `-q` here on purpose — you *want* the full Snyk output in the log.
- `-l` is a Maven flag (not a shell pipe), so it plays nicely with the one-command / no-pipe rule.
- After it finishes, **read `snyk-build.log` with the file reader** to see the complete Snyk
  report (vulnerable paths, introduced-through chains, and the fix version each CVE needs).
- The file is overwritten each run; never delete it first. Add it to `.gitignore` if not already.
- Use this whenever a Snyk gate fails so you can bump the *exact* offending dependency instead of
  guessing from a truncated console dump.

### Canonical commands — copy VERBATIM (do not modify)

| Purpose                              | Command                                                                      |
|--------------------------------------|------------------------------------------------------------------------------|
| Compile only                         | `mvn -q clean compile`                                                       |
| Fast test loop (skips the Snyk scan) | `mvn -q test -Dsnyk.skip`                                                    |
| One test class                       | `mvn -q test -Dsnyk.skip -Dtest=WebPipelineTest`                             |
| Several classes                      | `mvn -q test -Dsnyk.skip -Dtest=ClassA,ClassB`                               |
| A package                            | `mvn -q test -Dsnyk.skip -Dtest="be.dnsbelgium.mercator.pipeline.dns.*Test"` |
| Full build incl. Snyk vuln scan      | `mvn -q test`                                                                |
| Full build, capture Snyk report      | `mvn test -l snyk-build.log` (then read `snyk-build.log`)                    |
| Check dependency updates             | `mvn versions:display-dependency-updates -DprocessDependencyManagement=true` |
| Check managed-property updates       | `mvn versions:display-property-updates`                                      |
| Check build-plugin updates           | `mvn versions:display-plugin-updates`                                        |

Running all tests needs Docker for Testcontainers Postgres; DuckDB runs in-memory.

### Reading results (no shell pipes needed)
- `mvn -q` prints **nothing on success** and the `[ERROR] …` lines on failure.
  **Empty output ⇒ BUILD SUCCESS.**
- For a per-class summary, open the report file directly with the file reader:
  `target/surefire-reports/<fully.qualified.ClassName>.txt`.
  Maven overwrites these each run — never delete them first.

### Agent Command Conventions — READ THIS (avoids repeated "Allow once" prompts)

The terminal approval remembers a command **only by its exact string**; any variation forces a
new approval. Therefore:

1. **Use the canonical commands above VERBATIM** — same tool, same flags, same order, every time.
2. **One command per run. No chaining or filtering.** Never add `cd …`, `&&`, `;`,
   `| grep`, `| awk`, `| tail`, `| head`, `> file`, or `; echo $?`. The full output is already
   returned to you.
3. **Never use `./mvnw`.** Always `mvn`.
4. **Use the flags exactly as written** — `-Dsnyk.skip` (bare, not `-Dsnyk.skip=true`), and do
   not add `-o` or other ad-hoc `-D…`. If a task genuinely needs a new command, add that exact
   command to the table above first, then use it.
5. **If terminal output ever looks empty, just re-run the identical command** (occasional
   buffering). Do NOT switch to pipes/redirects to "see" it — that only creates new approvals.
   To inspect test outcomes, read `target/surefire-reports/*.txt` with the file reader.

### Commands to allowlist once (for the human)
Approving these exact strings covers the whole day-to-day loop:
- `mvn -q clean compile`
- `mvn -q test` (full build incl. Snyk)
- `mvn test -l snyk-build.log` (full build, full Snyk report captured to a log file)
- `mvn -q test -Dsnyk.skip` (fast test loop)
- `mvn -q test -Dsnyk.skip -Dtest=...` (the `-Dtest=` value varies; the prefix is stable)




# Agent Coding Quality Guide

Use this checklist when creating or changing code in this repository.

## 1) Understand Before You Change
- Locate existing patterns in production code and tests, then follow them.
- Keep scope tight: solve the requested issue and avoid unrelated refactors.

### Architecture (read `README.MD` first)
- Generic **producer → processor → writer** pipeline on Java 25 virtual threads:
  `ItemSource<I>` → `inputQueue` → `ItemProcessor<I,O>` (many virtual threads) → `resultQueue`
  → `ItemWriter<O>` (single thread). The engine is `PipelineService`; shared machinery and
  tuning live in `config/` (`PipelineExecutors`, `PipelineProperties`), interfaces in `service/`.
- Each crawler is a `PipelineModule` Spring bean, selected at startup by the `pipeline.modules`
  property (`simulated`, `web`, `dns`, `smtp`). CSV-backed modules extend `VisitRequestModule<O>`
  and only supply `name()`, `processor()` and `outputType()` — no engine changes. Each module
  lives in its own package (`web/`, `dns/`, `smtp/`).
- Writers: `FileWriterService` (demo text batches) and `JsonItemWriter<T>`, which rolls every
  `batchSize` JSON files into `batch_%04d.parquet` via DuckDB (`spring.datasource.url=jdbc:duckdb:...`).
- `DatabaseItemSource` (package `be.dnsbelgium.mercator.pipeline.queue`) is a stateful source behind the `postgres-queue` Spring profile; it needs
  the `PG*` env vars (exported in the environment) and a Postgres instance. Queue tuning lives under
  `pipeline.queue.*` (see `application.properties`); `QueueModuleRunner` + `CrawlTaskDispatcher` drive
  the continuous bounded-pass orchestration. The stateful claim/lease + reaper design is specced in
  `agent-tasks/` and proven by the Testcontainers (`postgres:17`) tests `ReservationBlockThenRecheckTest`,
  `DatabaseItemSourceTest` and `CrawlTaskDispatcherTest`.
- Virtual-thread rules: never pool virtual threads (`newVirtualThreadPerTaskExecutor()`); bound
  outbound concurrency with the per-module `Semaphore` (`maxConcurrentRequests`); offload CPU-bound
  parsing to the bounded `cpuPool`. See the full caveats in `README.MD`.

## 2) Build Small, Correct, and Readable Changes
- Prefer simple, explicit code over clever code.
- Keep methods focused and short; extract helpers when logic becomes hard to scan.
- Use clear names that describe intent.
- Avoid duplicated logic; reuse existing utilities where possible.

## 3) Testing Standards (Mandatory)
- Add or update tests for every behavior change.
- Cover both happy-path and failure/edge cases.
- Keep tests deterministic: no time/network randomness unless explicitly mocked.
- Prefer table-driven test data when many similar cases exist.

### Shared test helpers — no duplication
- Generic, reusable test utilities (filesystem/glob helpers, DuckDB/Parquet row counters, etc.)
  belong in **`be.dnsbelgium.mercator.pipeline.testsupport.TestSupport`**, not copy-pasted into
  individual test classes.
- Before writing a helper method in a test, check `TestSupport` first; if it exists, reuse it
  (it is statically imported via `import static ...TestSupport.*`). If it doesn't, add it there.
- Never keep two identical/near-identical helper methods across test classes — extract the one
  copy into `TestSupport` and delete the duplicates.


### Assertion Style
- **Use AssertJ assertions in tests** (`assertThat(...)`) instead of JUnit old-style assertions like `assertEquals`, `assertTrue`, and `assertFalse`.
- Prefer fluent, intention-revealing assertions (for example: `assertThat(result).isEqualTo(expected)` or `assertThat(errors).contains("...")`).

## 4) Error Handling and Diagnostics
- Fail fast on invalid input with clear error messages.
- Log useful context, but never log secrets or sensitive certificate/private key material.
- Preserve root causes when rethrowing exceptions.

## 5) Compatibility and Maintainability
- Keep public behavior backward-compatible unless the task explicitly requires a break.
- Update related docs/comments when behavior or assumptions change.
- Remove dead code introduced during experimentation before finalizing.

## 6) Delivery Checklist (Before Finishing)
- [ ] Code compiles.
- [ ] All affected tests pass locally (skipped/disabled tests are acceptable).
- [ ] New behavior is covered by tests.
- [ ] Assertions use AssertJ fluent style.
- [ ] No unrelated file changes were introduced.
- [ ] Documentation/task notes updated if needed.
