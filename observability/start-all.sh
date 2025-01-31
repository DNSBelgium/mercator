#!/bin/bash
set -e

docker compose pull
docker compose build
docker compose up --renew-anon-volumes --remove-orphans -d

docker compose logs | grep -i "Started"

docker compose ps

docker compose stats --no-stream

