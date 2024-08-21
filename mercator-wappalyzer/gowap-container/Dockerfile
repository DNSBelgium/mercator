FROM golang:latest AS gobuilder
RUN apk add git
RUN git clone https://github.com/DNSBelgium/gowap ./gowap && cd gowap && go build -o /gowap_bin cmd/gowap/main.go
