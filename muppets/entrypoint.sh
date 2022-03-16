#!/bin/sh
set -e

cp dist/config.json dist/config/config.json

if [ "$S3_ENDPOINT" != "UNDEFINED" ]; then
  sed -ri "s#\"(s3_endpoint)\":.+\"(,?)\$#\"\1\": \"${S3_ENDPOINT}\"\2#g" dist/config/config.json
else
  sed -ri "s#\"(s3_endpoint)\":.+\"(,?)\$#\"\1\": null\2#g" dist/config/config.json
fi

if [ "$S3_BUCKET" != "UNDEFINED" ]; then
  sed -ri "s#\"(s3_bucket_name)\":.+(\",?)\$#\"\1\": \"${S3_BUCKET}\2#g" dist/config/config.json
fi

if [ "$SQS_ENDPOINT" != "UNDEFINED" ]; then
  sed -ri "s#\"(sqs_endpoint)\":.+\"(,?)\$#\"\1\": \"${SQS_ENDPOINT}\"\2#g" dist/config/config.json
else
  sed -ri "s#\"(sqs_endpoint)\":.+\"(,?)\$#\"\1\": null\2#g" dist/config/config.json
fi

if [ "$SQS_INPUT_QUEUE" != "UNDEFINED" ]; then
  sed -ri "s#\"(sqs_input_queue)\":.+(\",?)\$#\"\1\": \"${SQS_INPUT_QUEUE}\2#g" dist/config/config.json
fi

if [ "$SQS_OUTPUT_QUEUE" != "UNDEFINED" ]; then
  sed -ri "s#\"(sqs_output_queue)\":.+(\",?)\$#\"\1\": \"${SQS_OUTPUT_QUEUE}\2#g" dist/config/config.json
fi

if [ "$SERVER_PORT" != "UNDEFINED" ]; then
  sed -ri "s#\"(server_port)\": ?[[:digit:]]+(,?)\$#\"\1\": ${SERVER_PORT}\2#g" dist/config/config.json
fi

echo "launched with "
echo "s3_endpoint: ${S3_ENDPOINT}; s3_bucket: ${S3_BUCKET};"
echo "sqs_endpoint: ${SQS_ENDPOINT}; sqs_input_queue: ${SQS_INPUT_QUEUE}; sqs_output_queue: ${SQS_OUTPUT_QUEUE}"
cat dist/config/config.json

exec "$@"
