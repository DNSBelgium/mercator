#!/usr/bin/env bash

echo "Creating queues"

export DEFAULT_REGION=eu-west-1

awslocal sqs create-queue --queue-name mercator-muppets-input
awslocal sqs create-queue --queue-name mercator-muppets-output
awslocal sqs create-queue --queue-name mercator-wappalyzer-input
awslocal sqs create-queue --queue-name mercator-wappalyzer-output
awslocal sqs create-queue --queue-name mercator-dns-crawler-input
awslocal sqs create-queue --queue-name mercator-content-crawler-input
awslocal sqs create-queue --queue-name mercator-dispatcher-input
awslocal sqs create-queue --queue-name mercator-dispatcher-output
awslocal sqs create-queue --queue-name mercator-dispatcher-ack
awslocal sqs create-queue --queue-name mercator-smtp-crawler-input
awslocal sqs create-queue --queue-name mercator-vat-crawler-input
awslocal sqs create-queue --queue-name mercator-tls-crawler-input
awslocal sqs set-queue-attributes --queue-url http://localhost:4566/000000000000/mercator-smtp-crawler-input --attributes VisibilityTimeout=1800
echo "Finished"
