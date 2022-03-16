#!/bin/bash

set -e
#set -x

rm -f keyStore.jks

# Generating a 4096 bit RSA private key (key.pem) and saving the public certificate in cert.pem
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 13650 -passout pass:password -subj '/C=AU/ST=SomeState/L=Atlantis/CN=testing.com'

# Using openssl to export the (certificate + private key) to a p12 keystore
openssl pkcs12 -export -out keyStore.jks -inkey key.pem -in cert.pem -passin pass:password -passout pass:password

# Using keytool to show contents of keystore.p12
echo "============= keyStore.jks ====================="
keytool -list -keystore keyStore.jks -storepass password
echo "================================================"

rm -f trustStore.jks
echo "Using keytool to import the public key (cert.pem) in a Java trustStore  (called trustStore.jks)"
keytool -import -file cert.pem -alias tls-testing -keystore trustStore.jks -storepass password -noprompt

echo "================= trustStore.jks ================="
keytool -list -keystore trustStore.jks -storepass password
echo "=================================================="
rm key.pem cert.pem
