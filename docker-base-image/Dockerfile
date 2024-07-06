FROM eclipse-temurin:21-alpine AS build

#RUN curl https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -o /etc/ssl/certs/rds-combined-ca-bundle.pem
#RUN keytool -genkey -keyalg RSA -alias localhost -validity 3650 -keysize 2048 -keypass changeit -keystore /etc/ssl/certs/keystore.jks -storepass changeit -dname "C=BE/ST=Vlaams-Brabant/L=Leuven/OU=DNS Belgium vzw/email=admin@dnsbelgium.be/CN=localhost" -storetype pkcs12


FROM eclipse-temurin:21-alpine

COPY --from=build /etc/ssl/certs /etc/ssl/certs

# CVE-2022-43680 fixed in expat 2.5.0-r0
# CVE-2023-0286
RUN apk upgrade --no-cache expat libcrypto1.1
RUN apk upgrade --no-cache libssl3
