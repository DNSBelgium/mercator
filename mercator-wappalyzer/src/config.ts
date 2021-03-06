const dotenv = require('dotenv');

dotenv.config();
export const config = {
    sqs_endpoint: process.env.SQS_ENDPOINT || '',
    sqs_input_queue: process.env.SQS_INPUT_QUEUE || '',
    sqs_output_queue: process.env.SQS_OUTPUT_QUEUE || '',
    server_port: process.env.SERVER_PORT || 8080
}
