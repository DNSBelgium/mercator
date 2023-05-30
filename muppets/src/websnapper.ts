import AWS from "aws-sdk";
import { ServiceConfigurationOptions } from "aws-sdk/lib/service";
import { Consumer } from "sqs-consumer";
import Producer from "sqs-producer";
import * as metrics from "./metrics.js";
import * as scraper from "./scraper.js";
import { config } from "./config.js";
import { computePath } from "./util.js";

const sqsOptions: ServiceConfigurationOptions = {};
if (config.sqs_endpoint) {
    sqsOptions.endpoint = config.sqs_endpoint;
}
const sqs = new AWS.SQS(sqsOptions);

const s3Options: ServiceConfigurationOptions = {};
if (config.s3_endpoint) {
    s3Options.endpoint = config.s3_endpoint;
    s3Options.s3ForcePathStyle = true;
}
const s3 = new AWS.S3(s3Options);

export async function getQueueUrl(queueName: string) {
    const queueUrlResponse = await sqs.getQueueUrl({ QueueName: queueName }).promise();
    return queueUrlResponse.QueueUrl;
}

getQueueUrl(config.sqs_input_queue).then(queueUrl => metrics.sqsMetrics(sqs, queueUrl!));

export function createProducer(queueName: string) {
    return Producer.create({
        queueUrl: queueName,
        sqs: sqs
    });
}

export function createConsumer(queueName: string, handleMessage: (message: AWS.SQS.Types.Message) => Promise<void>) {
    const consumer = new Consumer({
        queueUrl: queueName,
        sqs: sqs,
        visibilityTimeout: 60,
        waitTimeSeconds: 20,

        handleMessage: handleMessage
    });

    consumer.on("error", (err: Error) => {
        console.error("SQS error: " + err.message);
    });

    consumer.on("processing_error", (err: Error) => {
        console.error("processing_error: " + err.message);
    });

    return consumer;
}

function clean(result: scraper.ScraperResult) {
    delete result.htmlData;
    delete result.screenshotData;
    delete result.harData;

    return result;
}

export class S3FileUploader implements IFileUploader {
    async upload(data: string | void | Buffer, filename: string, prefix: string, uploadFileFormat: string, contentType?: string): Promise<string> {
        if (!data)
            return Promise.resolve("");

        const params = {
            Bucket: config.s3_bucket_name,
            Key: prefix + "/" + filename,
            Body: data,
            // ContentType: contentType
        };

        console.log("Uploading item [%s]", params.Key);

        return s3.upload(params).promise().then(putObjectPromise => {
            console.log("Key = [%s]", putObjectPromise.Key);
            return putObjectPromise.Key;
        }).catch(err => {
            throw new Error(`Upload failed for ${uploadFileFormat} file [${params.Key}] : [${JSON.stringify(err)}]`);
        });
    }
}

export interface IFileUploader {
    upload(data: string | void | Buffer, filename: string, prefix: string, uploadFileFormat: string, contentType?: string): Promise<string>;
}

export async function uploadScrapedData(result: scraper.ScraperResult, uploader: IFileUploader = new S3FileUploader) {
    const url: URL = new URL(result.request.url);
    result.bucket = config.s3_bucket_name;
    const prefix: string = computePath(url);
    const s3UploadResults: Promise<string>[] = [];

    console.log("Uploading to S3 [%s]", prefix);
    if (result.screenshotData !== undefined) {
        metrics.getScreenshotsSizes().observe((result.screenshotData?.length / 1024) / 1024);
        if (result.screenshotData.length < config.max_content_length) {
            s3UploadResults.push(uploader.upload(result.screenshotData, "screenshot.webp", prefix, "screenshot", "image/webp").then(key => result.screenshotFile = key).catch((err) => {
                result.errors.push(err.message);
                return Promise.reject()
            }));
            result.screenshotSkipped = false
        } else {
            metrics.getBigScreenshotCounter().inc()
            result.screenshotSkipped = true
        }
    }

    if (result.htmlLength !== undefined) {
        if (result.htmlLength < config.max_content_length) {
            s3UploadResults.push(uploader.upload(result.htmlData, result.pathname || "index.html", prefix, "html", "text/html").then(key => result.htmlFile = key).catch((err) => {
                result.errors.push(err.message);
                return Promise.reject();
            }));
            result.htmlSkipped = false;
        } else {
            result.htmlSkipped = true
        }
    }

    s3UploadResults.push(uploader.upload(result.harData, result.hostname + ".har", prefix, "har", "application/json").then(key => result.harFile = key).catch((err) => {
        result.errors.push(err.message);
        return Promise.reject();
    }));
    return Promise.all(s3UploadResults).then(() => result);
}

export async function handleMessage(message: AWS.SQS.Types.Message) {
    const params = JSON.parse(message.Body!);

    if (params.url) {
        const result = await scraper.websnap(params)
            .then(result => uploadScrapedData(result))
            .then(result => clean(result));

        if (result.errors.length)
            console.log("Scraper returned with the following error : %s", result.errors.join("\n"));

        metrics.getProcessedUrlCounter().inc();

        console.log("Scraper returned with result [%s]", JSON.stringify(result));
        return result;
    } else {
        console.error("ERROR: message on SQS did not have a URL specified.");
        console.error(message.Body!);
        console.error(params);
        console.error(params.url);
    }
}

export async function createHandler(producer: Producer): Promise<(message: AWS.SQS.Types.Message) => Promise<void>> {
    return async (message: AWS.SQS.Types.Message) => {
        const scraperResult = await handleMessage(message);
        if (scraperResult) {
            producer.send({
                id: scraperResult.id,
                body: JSON.stringify(scraperResult)
            }, function (err: Error) {
                if (err) console.log("Failed to send message to SQS: " + err);
            });
        }
    };
}
