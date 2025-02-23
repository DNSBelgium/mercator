# Mercator [![Run unit tests](https://github.com/DNSBelgium/mercator/actions/workflows/junit-report.yml/badge.svg)](https://github.com/DNSBelgium/mercator/actions/workflows/junit-report.yml) [![Check vulnerabilities](https://github.com/DNSBelgium/mercator/actions/workflows/snyk.yml/badge.svg)](https://github.com/DNSBelgium/mercator/actions/workflows/snyk.yml)

> [!WARNING]  
> This codebase is for Mercator v2 (also named Monocator because of its single container) and is still under development. Checkout the `v1.0` branch for the older and more stable codebase.

# Intro

Mercator 2 is a crawler based on [Mercator](https://github.com/DNSBelgium/mercator/tree/v1.0), but it has only one design goal: **ease of use**

# Getting started

## stand-alone

First, create a folder where Mercator can write its output:

```bash
export MERCATOR_DATA_DIR=~/mercator_data
mkdir -p ${MERCATOR_DATA_DIR} 
```

Now start Mercator in a container:

```bash
docker run --rm  -e SMTP_ENABLED=false -v ${MERCATOR_DATA_DIR}:/root/mercator -p 8082:8082 ghcr.io/dnsbelgium/mercator:latest
```

You can omit '-e SMTP_ENABLED=false' if you want to contact SMTP servers (but please know that it often takes a long time).

Every 20 seconds, Mercator will search for work by reading all CSV and parquet files in the ${MERCATOR_DATA_DIR}/work/ directory.

So feed it some work by copying a CSV file in that folder
```
export DATA_URL="https://raw.githubusercontent.com/DNSBelgium/mercator/refs/heads/main/sample_data/visit_requests.csv"
curl -JL -o ${MERCATOR_DATA_DIR}/work/visit_requests.csv $DATA_URL   
```

Now browse to http://localhost:8082/status to see the progress.

When all visits are done, you can stop the container.

See below for details on using the generated output.

## With docker compose

```bash
git clone git@github.com:DNSBelgium/mercator.git
cd mercator/observability
./start-all.sh
```

The [start-all.sh](./observability/start-all.sh) script will start three containers:
* Mercator: you can check its Web interface at http://localhost:8082
* Prometheus: to collect metrics from Mercator
* Grafana: to visualize the metrics, see http://localhost:3000

The Mercator container should have mounted a directory [mercator_data](./mercator_data) where you can find the crawled data.  
Every 20 seconds, Mercator will search for work by reading all CSV and parquet files in the mercator_data/work/ directory. 

So let's go back to the terminal to give Mercator something to do:
```
cp ../sample_data/visit_requests.csv ../mercator_data/work/  
```

That [csv file](./sample_data/visit_requests.csv) contains a thousand .be domains extracted from the Tranco list.

Now browse to http://localhost:8082/status to see the progress.
   
When all visits are done, you can stop the containers like this:

```
docker compose stop 
```

The result of the crawling is stored in parquet files in ../mercator/visits/exported/*/*.parquet.

You could use for example [duckdb](https://duckdb.org/#quickinstall) to read the data:

```
duckdb -c "select domain_name, title, twitter_links  from '../mercator_data/visits/exported/*/html_features.parquet' limit 10"  
```

```
duckdb -c "select domain_name, url, status_code from '../mercator_data/visits/exported/*/web_page_visit.parquet' limit 10"
```

```
duckdb -c "select ip, server_name, support_tls_1_3, support_ssl_3_0  from 'mercator_data/visits/exported/*/tls_full_scan.parquet'"
```

But there are many tools and libraries that support reading parquet files.
          
### Clean up

To destroy the containers:
```bash
docker compose down 
```
      
# Compared to version 1

Important differences compared to previous version of Mercator 
  
* Zero required dependencies to deploy it.
* Can be run as a single docker image 
* No longer requires a PhD in Kubernetes in order to deploy it ;-)
* Heck, it doesn't even need Kubernetes at all.
* Does not require any AWS services (but can optionally save its output on Amazon S3). 
* Uses an embedded [duckdb](https://duckdb.org/) database and writes its output as parquet files
* Uses an embedded ActiveMQ to distribute the work over multiple threads
* Only one Javascript dependency: [htmx](https://htmx.org/)
* Multi-platform docker images published to docker hub (x86 and aarch64) so it also works an Apple Silicon machines

# Compiling & running locally 

## If you have no (recent) JDK on your system

```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.5-tem
sdk install maven 3.9.9 
```

## Compiling

```
mvn package
```
Will compile the sources and run all (enabled) tests. To run the tests, you need a Docker environment.

To run without tests and vulnerability scanning, use:
```bash
mvn package -DskipTests -Dsnyk.skip
```

## Running locally compiled version

```
mvn spring-boot:run
```

To use a specific profile: 
```
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

>[!NOTE]
> Using the 'local' profile will start mercator on port 8090 instead of 8082.

## Running the JAR file
```
java -jar -Dspring.active.profiles=local  target/mercator-*-SNAPSHOT.jar
```

>[!NOTE]
> You need to run `mvn package` first.

## Java 23 & Lombok

Since Lombok is [not yet compatible](https://github.com/projectlombok/lombok/issues/3722) with JDK 23, we *compile* the sources with Java 21.
Once compiled, it is possible to *run* the application with Java 23.

# Running with docker compose

Build container image:
```
mvn jib:dockerBuild
```

* Store a username in ~/.env.grafana-username
* Store a password in ~/.env.grafana-password

```
cd ./observability
docker-compose up --renew-anon-volumes --remove-orphans -d
docker compose logs monocator 
```

The Mercator UI should be available at http://localhost:8082

Metrics are available on http://localhost:3000 
                 
# Instructions for running it in production

Will follow soon.

# Features
  
Mercator will do the following info for each submitted domain name
* Fetch a configurable set of DNS resource records (SOA, A, AAAA, NS, MX, TXT, CAA, HTTPS, SVCB, DS, DNSKEY, CDNSKEY, CDS, ...)
* Fetch one or more html pages
* Extract features from all collected html pages
* Record conversations with all configured SMTP servers
* Check the TLS versions (and cipher suites) supported on port 443
* Find work by scanning a configurable folder for parquet files with domain names

Other features:
* Publish metrics for Prometheus
* docker-compose file to start Prometheus & [Grafana](http://localhost:3000)  
  * with custom Grafana dashboards with the most important metrics

## Planned
* export output files to S3
* optionally receive work via SQS

## In progress
* show fetched data in web ui
