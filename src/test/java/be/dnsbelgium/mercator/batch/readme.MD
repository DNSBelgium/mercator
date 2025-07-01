
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
* for tables with only simple datatypes we can create an ItemWriter that uses a DuckDbAppender and avoid the json step in between.
* when done (TBD: with a chunk or with a job): use duckdb to convert the JSON files to parquet 
* optionally upload the parquet files to S3. 

## To be decided
* either use @Scheduled every x minutes (Java process keeps running) 
* or define a K8S cronjob to start the Batch jobs (Java process stops when no more files to process)
* or support both
  * spring profile : batch => no webgui and stop when done
  * spring profile : web => webgui and @scheduled: to be checked how does job work when no CSV is found 


# Potential issues
* Changing the layout of classes like WebCrawlResult will result in (unintentional) differences in output structure  
* But this can be covered with unit tests
