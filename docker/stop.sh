export repo="ghcr.io/dnsbelgium/mercator"

container_id=$(docker ps --filter ancestor=$repo --format {{.ID}})

docker logs ${container_id}

echo "Stopping container with id: [${container_id}]"

docker stop ${container_id}

docker ps

