import dotenv from "dotenv";
dotenv.config();

import { log } from "./logging.js";

interface MuppetsConfiguration {
    s3_endpoint: string | null,
    s3_bucket_name: string,
    sqs_endpoint: string | null,
    sqs_input_queue: string,
    sqs_output_queue: string,
    server_port: number,
    verbose: boolean,
    max_content_length: number,
    failure_threshold: number
};

const MAX_CONTENT_LENGTH_DEFAULT = "10485760";

const config: MuppetsConfiguration = {
    s3_endpoint: process.env.S3_ENDPOINT || null,
    s3_bucket_name: process.env.S3_BUCKET || "mercator-muppets",
    sqs_endpoint: process.env.SQS_ENDPOINT || null,
    sqs_input_queue: process.env.SQS_INPUT_QUEUE || "mercator-muppets-input",
    sqs_output_queue: process.env.SQS_OUTPUT_QUEUE || "mercator-muppets-output",
    server_port: parseInt(process.env.SERVER_PORT || "8085"),
    verbose: (process.env.VERBOSE !== undefined && process.env.VERBOSE.toLowerCase() == 'true'),
    max_content_length: parseInt(process.env.MAX_CONTENT_LENGTH || MAX_CONTENT_LENGTH_DEFAULT),
    failure_threshold: parseFloat(process.env.FAILURE_THRESHOLD || '0.05')
};

log(`Using configuration`);
log(config);

export { config };
