#!/usr/bin/env bash

echo "Creating buckets"

export DEFAULT_REGION=eu-west-1

bucket_name=mercator-muppets
awslocal s3 mb s3://${bucket_name}
awslocal s3api put-bucket-acl --bucket ${bucket_name} --acl public-read-write

echo "Finished"
