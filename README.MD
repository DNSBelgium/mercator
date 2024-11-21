[![CodeQL](https://github.com/maartenbosteels/monocator/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/maartenbosteels/monocator/actions/workflows/github-code-scanning/codeql)

[![Run unit tests](https://github.com/maartenbosteels/monocator/actions/workflows/junit-report.yml/badge.svg)](https://github.com/maartenbosteels/monocator/actions/workflows/junit-report.yml)

# Intro

Monocator is a crawler based on [Mercator](https://github.com/DNSBelgium/mercator)
but it has only one design goal: **ease of use**

# Getting started

```bash
docker run -p 8082:8082 maartenbosteels/monocator:latest
open localhost:8082 
```
                   
# Compared to Mercator

Important differences compared to current version of Mercator 
  
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

## Compiling
```
mvn package
```
Will compile the sources and run all (enabled) tests.

## Running locally compiled version

```
mvn spring-boot:run
```

To use a specific profile: 
```
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Note: using the 'local' profile will start monocator on port 8090 instead of 8082.

## Running the JAR file
```
java -jar -Dspring.active.profiles=local  target/monocator-0.0.3-SNAPSHOT.jar
```

Note: you need to run `mvn package` first.

## Java 23 & Lombok

Since Lombok is [not yet compatible](https://github.com/projectlombok/lombok/issues/3722) with JDK 23, we *compile* the sources with Java 21.
Once compiled, it is possible to *run* the application with Java 23.

# Running with docker compose

* Store a username in ~/.env.grafana-username
* Store a password in ~/.env.grafana-password

```
cd ./observability
docker-compose up --renew-anon-volumes --remove-orphans -d
docker compose logs monocator 
```

The Monocator UI should be available at http://localhost:8082

Metrics are available on http://localhost:3000 
                 
# Instructions for running it in production

Will follow soon.

# Features
  
Monocator will do the following info for each submitted domain name
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
