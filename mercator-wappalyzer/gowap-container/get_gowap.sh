#!/bin/sh
# Script to locally get a gowap binary that you can use to run the module.
set -e

IMAGE_NAME=gowap_builder
CONTAINER_NAME=gowap_builder_instance
DESTINATION_PATH=../gowap


docker build -t $IMAGE_NAME -f ./Dockerfile .
docker run --name $CONTAINER_NAME $IMAGE_NAME
docker cp $CONTAINER_NAME:/gowap_bin $DESTINATION_PATH
docker rm -f $CONTAINER_NAME

echo "File extracted to $DESTINATION_PATH"
