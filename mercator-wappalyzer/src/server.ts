import { Message, SQS } from "@aws-sdk/client-sqs";
import { SQSClientConfig } from "@aws-sdk/client-sqs/dist-types/SQSClient";
import { Consumer } from "sqs-consumer";
import { Producer } from "sqs-producer";
import { analyze } from "./gowap_wrapper";
import { WappalyzerRequest, WappalyzerResponse } from "./api";
import { v4 as uuid } from "uuid";
import { debug, error, log } from "./logging";

async function getQueueUrl(sqs: SQS, queueName: string) {
    const queueUrlResponse = await sqs.getQueueUrl({ QueueName: queueName });
    return queueUrlResponse.QueueUrl;
}

export async function start_sqs_polling(endpoint: string, input_queue: string, output_queue: string) {
    const sqsOptions: SQSClientConfig = {};
    if (endpoint) {
        sqsOptions.endpoint = endpoint;
    }
    const sqs = new SQS(sqsOptions)

    let output_queue_url = await getQueueUrl(sqs, output_queue)
    let input_queue_url = await getQueueUrl(sqs, input_queue)
    log("input_queue_url: " + input_queue_url)
    log("output_queue_url: " + output_queue_url)

    if (!output_queue_url) {
        throw "Could not determine input_queue_url"
    }
    if (!input_queue_url) {
        throw "Could not determine output_queue_url"
    }

    const producer = Producer.create({
        queueUrl: output_queue_url,
        sqs: sqs
    });
    const consumer = new Consumer({
        queueUrl: input_queue_url,
        sqs: sqs,
        visibilityTimeout: 60,
        waitTimeSeconds: 20,
        handleMessage: await createHandler(producer)
    });
    consumer.on("error", (err: Error) => {
        error("SQS error: " + err.message);
        throw err;
    });
    consumer.on("processing_error", (err: Error) => {
        error("processing_error: " + err.message);
        throw err;
    });
    consumer.start();
    debug("consumer.start() is done")
    debug("consumer.isRunning() : " + consumer.isRunning)
}

export async function createHandler(producer: Producer): Promise<(message: Message) => Promise<void>> {
    return async (message: Message) => {
        const params = JSON.parse(message.Body!);
        const result = await handleMessage(params);
        if (result) {
            await producer.send({
                id: uuid(),
                body: JSON.stringify(result)
            }).catch(err => {
                error("Failed to send message to SQS: " + err);
                throw err;
            });
        }
    };
}

export async function handleMessage(request: WappalyzerRequest): Promise<WappalyzerResponse> {
    return {
        request: request,
        wappalyzer: await analyze(request.url)
    }
}
