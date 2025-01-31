export repo="ghcr.io/dnsbelgium/mercator"

export MERCATOR_DATA_DIR=~/mercator_data
mkdir -p ${MERCATOR_DATA_DIR}

container_id=$(docker run --rm  -v ${MERCATOR_DATA_DIR}:/root/mercator -p 8082:8082  -d $repo)
export container_id
echo "container_id: $container_id"

./logs.sh