const dotenv = require('dotenv');

dotenv.config();
export const config = {
    sqs_endpoint: process.env.SQS_ENDPOINT || '',
    sqs_input_queue: process.env.SQS_INPUT_QUEUE || '',
    sqs_output_queue: process.env.SQS_OUTPUT_QUEUE || '',
    failure_threshold: parseFloat(process.env.FAILURE_THRESHOLD || '0.05'),
    server_port: parseInt(process.env.SERVER_PORT || '8080')
}
