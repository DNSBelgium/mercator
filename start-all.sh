#!/bin/bash
set -e

./gradlew dockerBuild

docker-compose build
LOCALSTACK_HOSTNAME=localstack docker-compose up --renew-anon-volumes --remove-orphans
