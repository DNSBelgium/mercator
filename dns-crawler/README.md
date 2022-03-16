# DNS Crawler

Gathers DNS records. Which records from which subdomain can be easily configured. `@` represent the apex (`[]` is used here to escape the character `@`, see [documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding.maps))

```shell
crawler.dns.subdomains.[@]=A, AAAA, MX, SOA, TXT, CAA, HTTPS, SVCB
crawler.dns.subdomains.www=A, AAAA
crawler.dns.subdomains._dmarc=TXT
```

Result is stored in json, for example

```json
{
    "@": {
        "records": {
            "A": [
                "45.60.74.42"
            ],
            "MX": [
                "0 dnsbelgium-be.mail.protection.outlook.com."
            ],
            "CAA": [
                "0 issue \"amazon.com\"",
                "0 issue \"letsencrypt.org\"",
                "0 iodef \"mailto:cert-abuse@dnsbelgium.be\"",
                "0 issue \"globalsign.com\""
            ],
            "SOA": [
                "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2022094375 10800 1800 3600000 3600"
            ],
            "TXT": [
                "\"spf2.0/mfrom,pra include:spf.protection.outlook.com include:qlan.eu include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\"",
                "\"apple-domain-verification=1bO1oU8ux8xGmGqT\"",
                "\"QHW9u39wLyjqPCFmoNpDsDJHubOneJ2Eecw5Xt+DljI=\"",
                "\"_globalsign-domain-verification=aGlGYgHuFYu0D2FqVnKKkORIcIB2uvzfp8u9aXdQ9m\"",
                "\"v=spf1 include:spf.protection.outlook.com include:qlan.eu include:_spf.elasticemail.com include:servers.mcsv.net include:spf.flexmail.eu ip4:52.17.217.28 ip4:52.214.17.58 ip4:84.199.48.136 -all\"",
                "\"miro-verification=0025c3eb22c1eef7625c0a52e3c262ccb937b6fa\""
            ],
            "AAAA": [
                "2a02:e980:8f:0:0:0:0:2a"
            ],
            "SVCB": [],
            "HTTPS": []
        }
    },
    "www": {
        "records": {
            "A": [
                "45.60.74.42"
            ],
            "AAAA": [
                "2a02:e980:8f:0:0:0:0:2a"
            ]
        }
    },
    "_dmarc": {
        "records": {
            "TXT": [
                "\"v=DMARC1; p=quarantine; pct=10; fo=0; rua=mailto:dmarc@dnsbelgium.be; ruf=mailto:dmarc@dnsbelgium.be\""
            ]
        }
    }
}
```

It also add information about GeoIP on A and AAAA IPs such as AS number, AS organisation and country.