import Prometheus from "prom-client";
import AWS from "aws-sdk";
import { error } from "./logging";

Prometheus.collectDefaultMetrics({ prefix: "muppets_" });

async function calculateFailureRate(): Promise<number> {
    const failed = await getValueOfMetric(getFailedToProcess());
    const okay = await getValueOfMetric(getProcessedUrlCounter());
    const total = failed + okay;

    if (total == 0)
        return 0.0;

    return failed / total;
}

async function getValueOfMetric(metric: Prometheus.Counter): Promise<number> {
    return (await metric.get()).values[0].value;
}

function getContentType() {
    return Prometheus.register.contentType;
}

function getMetrics() {
    return Prometheus.register.metrics();
}

const urlProcessedCounter = new Prometheus.Counter({
    name: "muppets_url_processed_counter",
    help: "Number of processed URL"
});

const failedToProcess = new Prometheus.Counter({
    name: "muppets_failed_to_process",
    help: "Failures of both snapping and processing (e.g. uploading to the storage bucket)"
});
function getFailedToProcess() {
    return failedToProcess;
}

function getProcessedUrlCounter() {
    return urlProcessedCounter;
}

const processingTimeHist = new Prometheus.Histogram({
    name: "muppets_snap_processing_time",
    help: "Processing time to scrap an URL"
});

function getProcessingTimeHist() {
    return processingTimeHist;
}

const urlToProcessGauge = new Prometheus.Gauge({
    name: "muppets_sqs_url_to_process_gauge",
    help: "Approximative number in the input queue"
});

const domainTimeOuts = new Prometheus.Counter({
    name: "muppets_timeout_domains",
    help: "Amount of timeouts from this server"
});

// prometeus bucket for keeping a record of screenschot sizes

const screenshotSizes = new Prometheus.Histogram({
    name: "muppets_screenshot_sizes",
    help: "sizes of screenshots recorded during scrape"
});

function getScreenshotsSizes() {
    return screenshotSizes;
}

// prometeus bucket for counter where the screenshot get counted that are above a certain size
function getBigScreenshotCounter() {
    return bigScreenshotCounter;
}

const bigScreenshotCounter = new Prometheus.Counter({
    name: "muppets_largerScreenshot_Counter",
    help: "Amount screenshots bigger then 10 MiB"
})

function getDomainTimeOuts() {
    return domainTimeOuts;
};

// Helper to compute metrics

function getSqsMetrics(sqs: AWS.SQS, queueUrl: string) {
    sqs.getQueueAttributes({ QueueUrl: queueUrl, AttributeNames: ["ApproximateNumberOfMessages"] })
        .promise()
        .then(data => urlToProcessGauge.set(parseInt(data.Attributes!.ApproximateNumberOfMessages)))
        .catch(() => error("Cannot retrieve SQS metrics !"));
}

function sqsMetrics(sqs: AWS.SQS, queueUrl: string) {
    setInterval(() => getSqsMetrics(sqs, queueUrl), 1000);
}

export { getContentType, getMetrics, getFailedToProcess as getFailureCounter, getProcessedUrlCounter, getProcessingTimeHist, getScreenshotsSizes, getBigScreenshotCounter, getDomainTimeOuts, sqsMetrics, calculateFailureRate };
