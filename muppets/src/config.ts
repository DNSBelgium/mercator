interface MuppetsConfiguration {
    s3_endpoint: string | null,
    s3_bucket_name: string,
    sqs_endpoint: string | null,
    sqs_input_queue: string,
    sqs_output_queue: string,
    server_port: number
};

function parse_env_string_or_default<T>(name: string, default_value: T): string | T {
    const value = process.env[name];
    if ((value === undefined) || (value.toUpperCase() === "UNDEFINED"))
        return default_value;
    else
        return value;
}

function parse_env_number_or_default<T>(name: string, default_value: T): number | T {
    const value = process.env[name];
    if ((value === undefined) || (value.toUpperCase() === "UNDEFINED"))
        return default_value;
    else
        return parseInt(value);
}

const env_config: MuppetsConfiguration  = {
    s3_endpoint: parse_env_string_or_default("S3_ENDPOINT", null),
    s3_bucket_name: parse_env_string_or_default("S3_BUCKET_NAME", "mercator-muppets"),
    sqs_endpoint: parse_env_string_or_default("SQS_ENDPOINT", null),
    sqs_input_queue: parse_env_string_or_default("SQS_INPUT_QUEUE", "mercator-muppets-input"),
    sqs_output_queue: parse_env_string_or_default("SQS_OUTPUT_QUEUE", "mercator-muppets-output"),
    server_port: parse_env_number_or_default("SERVER_PORT", 8085),
};

console.log(`Using configuration:`);
console.log(env_config);

export default env_config;
