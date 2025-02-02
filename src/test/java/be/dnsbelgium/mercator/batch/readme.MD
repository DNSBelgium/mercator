
# Observations
* Have multiple threads that are inserting rows in duckdb tables seems to be slow.
* The code to rotate the duckdb files every x visits is not very elegant.
* Doing a commit for every visit seems overkill (and it's slow).
* Especially since the thread is blocked until all modules are done for the current visit.
* Duckdb does not advice to use INSERT statements for bulk loading
  
> INSERT statements are the standard way of loading data into a database system. 
> They are suitable for quick prototyping, but should be avoided for bulk loading as they 
> have significant per-row overhead.

* We use ActiveMQ to spread the work over multiple threads. 
  On the one hand it requires little code, on the other hand we have very litlle control over it.

# Proposal

* use Spring Batch to read a CSV file with VisitRequests
* use a [VirtualThreadTaskExecutor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/task/VirtualThreadTaskExecutor.html) 
  to spread the crawling work over mutiple (virtual) threads.
* define a Batch job for each crawler module
* ItemReader : read from CSV file(s)
* implement an ItemProcessor for each crawler module:
   * DNS 
   * WEB (+ feature extraction)
   * SMTP
   * TLS
* ItemWriter: use a JsonFileItemWriter to write the crawl results to a JSON file
* when done (TBD: with a chunk or with a job): use duckdb to convert the JSON files to parquet 
* optionally upload the parquet files to S3. 

## To be decided
* either use @Scheduled every x minutes (Java process keeps running) 
* or define a K8S cronjob to start the Batch jobs (Java process stops when no more files to process)

## JSON to parquet

Suppose the batch jobs writes WebCrawlResult to WebCrawlResult.json
Do these steps to create parquet files:

create table WebCrawlResult as select * from 'WebCrawlResult.json';
create table pageVisits as select unnest(pageVisits) as s from WebCrawlResult;
copy (select s.* from pageVisits) to 'pageVisits.parquet';

create table htmlFeatures as select unnest(htmlFeatures) as s from WebCrawlResult;
copy (select s.* from htmlFeatures) to 'htmlFeatures.parquet';

copy (
  select * 
        exclude(pageVisits, htmlFeatures) 
        replace(
            to_timestamp(crawlStarted)::timestamp as crawlStarted, 
            to_timestamp(crawlFinished) as crawlFinished 
        ) 
  from WebCrawlResult
) to web_site_visit.parquet;

# Benefits

* All the code to create (duckdb) table can be removed.
* All the code to insert rows into these tables can be removed (serialization will be done by Jackson)
* All modules (at least WEB, SMTP, TLS) can be run in parallel and independed from each other
* No more commit per visit => should lead to higher throughput

# Potential issues
* Changing the layout of classes like WebCrawlResult will result in (unintentional) differences in output structure  
* But this can be covered with unit tests
