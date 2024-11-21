# container_id=$(docker ps --filter ancestor=maartenbosteels/monocator --format '{{json .ID}}' | jq .)

container_id=$(docker ps --filter ancestor=maartenbosteels/monocator --format {{.ID}})

docker logs ${container_id}

echo "Stopping container with id: [${container_id}]"

docker stop ${container_id}

docker ps

