export repo="maartenbosteels/monocator"
mkdir -p ~/monocat

container_id=$(docker run --rm  -v ~/monocat:/root/monocator -p 8082:8082  -d $repo)
export container_id
echo "container_id: $container_id"

./logs.sh