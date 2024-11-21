container_id=$(docker ps --filter ancestor=maartenbosteels/monocator --format {{.ID}})

docker ps

echo "==============================================================================="
echo "= Following at the logs, you can ctrl-c this without stopping the container  ="
echo "=============================================================================="

docker logs -f $container_id

docker ps

