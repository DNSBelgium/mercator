
# Intro

Mercator 2 is a crawler based on [Mercator](https://github.com/DNSBelgium/mercator), but it has only one design goal: **ease of use**

# Getting started

```bash
docker run -p 8082:8082 ghcr.io/dnsbelgium/mercator/mercator:latest
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

Note: using the 'local' profile will start mercator on port 8090 instead of 8082.

## Running the JAR file
```
java -jar -Dspring.active.profiles=local  target/mercator-*-SNAPSHOT.jar
```

Note: you need to run `mvn package` first.

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
