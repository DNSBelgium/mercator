FROM eclipse-temurin:17-jre AS build

RUN curl https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem -o /etc/ssl/certs/rds-combined-ca-bundle.pem
RUN keytool -genkey -keyalg RSA -alias localhost -validity 3650 -keysize 2048 -keypass changeit -keystore /etc/ssl/certs/keystore.jks -storepass changeit -dname "C=BE/ST=Vlaams-Brabant/L=Leuven/OU=DNS Belgium vzw/email=admin@dnsbelgium.be/CN=localhost" -storetype pkcs12

# Decided to use alpine since
# eclipse-temurin:17-jre         => ubuntu 22.04    => CVE-2022-3602 & CVE-2022-3786 (libssl3 and openssl) with no fix currently
# eclipse-temurin:17-jre-centos7 => centos 7.9.2009 => CVE-2015-8385 & CVE-2016-3191 (glib2 and pcre) with no fix currently

FROM eclipse-temurin:17-jre-alpine

COPY --from=build /etc/ssl/certs /etc/ssl/certs

# CVE-2022-43680 fixed in expat 2.5.0-r0
# CVE-2023-0286
RUN apk upgrade --no-cache expat libssl3 libssl1.1
