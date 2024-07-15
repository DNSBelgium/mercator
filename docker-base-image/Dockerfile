FROM eclipse-temurin:21-alpine@sha256:68a8a4ad547e750f497824540d90ff29d4b819a6a6287a5eb1b03a71e4c2167b as build

RUN wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /etc/ssl/certs/rds-combined-ca-bundle.pem
RUN keytool -genkey -keyalg RSA -alias localhost -validity 3650 -keysize 2048 -keypass changeit -keystore /etc/ssl/certs/keystore.jks -storepass changeit -dname "C=BE/ST=Vlaams-Brabant/L=Leuven/OU=DNS Belgium vzw/email=admin@dnsbelgium.be/CN=localhost" -storetype pkcs12


FROM eclipse-temurin:21-alpine@sha256:68a8a4ad547e750f497824540d90ff29d4b819a6a6287a5eb1b03a71e4c2167b

COPY --from=build /etc/ssl/certs /etc/ssl/certs

# CVE-2022-43680 fixed in expat 2.5.0-r0
# CVE-2023-0286
RUN apk upgrade --no-cache expat libcrypto1.1
RUN apk upgrade --no-cache libssl3
