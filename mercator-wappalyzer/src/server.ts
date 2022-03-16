import AWS from "aws-sdk";
import {ServiceConfigurationOptions} from "aws-sdk/lib/service";
import {Consumer} from "sqs-consumer";
import {Producer} from "sqs-producer";
import {wappalyze} from "./wappalyzer_wrapper";
import {WappalyzerRequest, WappalyzerResponse} from "./api";
import {v4 as uuid} from "uuid";

async function getQueueUrl(sqs: AWS.SQS, queueName: string) {
    const queueUrlResponse = await sqs.getQueueUrl({QueueName: queueName}).promise();
    return queueUrlResponse.QueueUrl;
}

export async function start_sqs_polling(endpoint: string, input_queue: string, output_queue: string) {
    const sqsOptions: ServiceConfigurationOptions = {};
    if (endpoint) {
        sqsOptions.endpoint = endpoint;
    }
    const sqs = new AWS.SQS(sqsOptions);

    const producer = Producer.create({
        queueUrl: await getQueueUrl(sqs, output_queue),
        sqs: sqs
    });

    const consumer = new Consumer({
        queueUrl: await getQueueUrl(sqs, input_queue),
        sqs: sqs,
        visibilityTimeout: 60,
        waitTimeSeconds: 20,
        handleMessage: await createHandler(producer)
    });

    consumer.on("error", (err: Error) => {
        console.error("SQS error: " + err.message);
        throw err;
    });

    consumer.on("processing_error", (err: Error) => {
        console.error("processing_error: " + err.message);
        throw err;
    });

    consumer.start();
}

export async function createHandler(producer: Producer): Promise<(message: AWS.SQS.Types.Message) => Promise<void>> {
    return async (message: AWS.SQS.Types.Message) => {
        const params = JSON.parse(message.Body!);
        const result = await handleMessage(params);
        if (result) {
            await producer.send({
                id: uuid(),
                body: JSON.stringify(result)
            }).catch(err => {
                console.error("Failed to send message to SQS: " + err);
                throw err;
            });
        }
    };
}

export async function handleMessage(request: WappalyzerRequest): Promise<WappalyzerResponse> {
    return {
        request: request,
        wappalyzer: await wappalyze(request.url)
    }
}
