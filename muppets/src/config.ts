interface MuppetsConfiguration {
    s3_endpoint: string | null,
    s3_bucket_name: string,
    sqs_endpoint: string | null,
    sqs_input_queue: string,
    sqs_output_queue: string,
    server_port: number
};

const env_config: MuppetsConfiguration  = {
    s3_endpoint: process.env.S3_ENDPOINT || null,
    s3_bucket_name: process.env.S3_BUCKET || "mercator-muppets",
    sqs_endpoint: process.env.SQS_ENDPOINT || null,
    sqs_input_queue: process.env.SQS_INPUT_QUEUE || "mercator-muppets-input",
    sqs_output_queue: process.env.SQS_OUTPUT_QUEUE || "mercator-muppets-output",
    server_port: parseInt(process.env.SERVER_PORT || "8085"),
};

console.log(`Using configuration:`);
console.log(env_config);

export default env_config;
