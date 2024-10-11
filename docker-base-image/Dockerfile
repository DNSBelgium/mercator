FROM eclipse-temurin:21-alpine@sha256:cf94706ed7b63f1f29b720182fe3385f2fd5d17b3a20ff60163ea480572d34c7 AS build

RUN wget https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /etc/ssl/certs/rds-combined-ca-bundle.pem
RUN keytool -genkey -keyalg RSA -alias localhost -validity 3650 -keysize 2048 -keypass changeit -keystore /etc/ssl/certs/keystore.jks -storepass changeit -dname "C=BE/ST=Vlaams-Brabant/L=Leuven/OU=DNS Belgium vzw/email=admin@dnsbelgium.be/CN=localhost" -storetype pkcs12


FROM eclipse-temurin:21-alpine@sha256:cf94706ed7b63f1f29b720182fe3385f2fd5d17b3a20ff60163ea480572d34c7

COPY --from=build /etc/ssl/certs /etc/ssl/certs

RUN apk upgrade && \
    apk upgrade libexpat
