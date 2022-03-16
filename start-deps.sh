#!/bin/bash
set -e

# using localhost as LOCALSTACK_HOSTNAME, to allow java apps running locally to connect to it
LOCALSTACK_HOSTNAME=localhost docker-compose up --renew-anon-volumes --remove-orphans localstack db db-admin prometheus grafana
