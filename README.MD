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
                 
# Instructions for running it in production

Will follow soon.

# Features
  
It fetches the following info for each submitted domain name
* Fetches configurable set of DNS resource records (SOA, A, AAAA, NS, MX, TXT, CAA, HTTPS, SVCB, DS, DNSKEY, CDNSKEY, CDS, ...)
* Fetches one or more html pages
* Extracts features from all collected html pages
* Records conversations with all configured SMTP servers
* Checks the TLS versions (and cipher suites) supported on port 443
* Finds work by scanning a configurable folder for parquet files with domain names
* Publishes metrics for Prometheus
* docker-compose file to start Prometheus Grafana with custom Grafana dashboards with most important metrics

Planned
* export output files to S3
* optionally receive work via SQS
* show fetched data in web ui

