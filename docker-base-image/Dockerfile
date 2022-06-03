FROM eclipse-temurin:11-jre AS build

RUN curl https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem -o /etc/ssl/certs/rds-combined-ca-bundle.pem
RUN keytool -genkey -keyalg RSA -alias localhost -validity 3650 -keysize 2048 -keypass changeit -keystore /etc/ssl/certs/keystore.jks -storepass changeit -dname "C=BE/ST=Vlaams-Brabant/L=Leuven/OU=DNS Belgium vzw/email=admin@dnsbelgium.be/CN=localhost" -storetype pkcs12

FROM eclipse-temurin:11-jre
COPY --from=build /etc/ssl/certs /etc/ssl/certs
