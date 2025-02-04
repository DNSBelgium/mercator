export repo="dnsbelgium/mercator"

container_id=$(docker ps --filter ancestor=$repo --format {{.ID}})

docker ps

echo "==============================================================================="
echo "= Following at the logs, you can ctrl-c this without stopping the container  ="
echo "=============================================================================="

docker logs -f $container_id

docker ps

