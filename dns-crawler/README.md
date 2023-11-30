# DNS Crawler

Gathers DNS records. Which records from which subdomain can be easily configured. `@` represent the apex (`[]` is used here to escape the character `@`, see [documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding.maps))

```shell
crawler.dns.subdomains.[@]=A, AAAA, MX, SOA, TXT, CAA, HTTPS, SVCB, NS, DS, DNSKEY, CDNSKEY, CDS
crawler.dns.subdomains.www=A, AAAA
crawler.dns.subdomains._dmarc=TXT
crawler.dns.subdomains._doomainkey=TXT
```

DNS Records are split into two separate tables, Requests and Responses.
Requests represents a DNS Record request.
It stores:
 - Visit Id
 - Domain Name
 - Prefix
 - Record Type (see crawler.dns.subdomains above for supported types)
 - rcode
 - Crawl's timestamp
 - Ok
 - Problem
 - Number of Responses

A Request would hold the following data as an example:

| id  | visit_id | domain_name | prefix | record_type   | rcode   | crawl_timestamp | ok      | problem    | num_of_responses |
|-----|----------|-------------|--------|---------------|---------|-----------------|---------|------------|------------------|
| 1   | [UUID]   | test000.be  |    @   | A             | 3       |    [timestamp]  | false   | nxdomain   | 0                |
| 2   | [UUID]   | realdom.be  |    @   | A             | 0       |    [timestamp]  | true    | <null>     | 2                |
| 3   | [UUID]   | realdom.be  |    @   | AAAA          | 0       |    [timestamp]  | true    | <null>     | 0                |
| 4   | [UUID]   | realdom.be  |    @   | SOA           | 0       |    [timestamp]  | true    | <null>     | 1                |

Id #3 did not get a Response while #2 and #4 did.
num_of_responses represents the amount of records found for a specific record_type.

A Response consists of:
 - Record Data
 - Time-To-Live

Request #2 has 2 Responses which could hold the following data as an example:

| id  | record_data | ttl  | request_id |
|-----|-------------|------|------------|
| 1   | 12.23.34.45 | 3600 | 2          |
| 2   | 45.34.23.12 | 3600 | 2          |


RRSIG data for (currently only) the SOA record type is saved in the record_signature table.

# GeoIP configuration

The following GeoIP information for the A and AAAA record types' Responses is also stored:
- Asn
- Country
- Ip
- Asn Organisation
- Ip Version

To store these values, you need a MaxMind license key and accompanying database files. The GeoLite license can be obtained from [https://maxmind.com](MaxMind) and must be stored in `MAXMIND_LICENSE_KEY` or the application properties.
