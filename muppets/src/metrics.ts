import Prometheus from "prom-client";
import AWS from "aws-sdk";

Prometheus.collectDefaultMetrics({prefix: "muppets_"});

export const getContentType = () => Prometheus.register.contentType;
export const getMetrics = () => Prometheus.register.metrics();

// Metrics type
const urlProcessedCounter = new Prometheus.Counter({
    name: "muppets_url_processed_counter",
    help: "Number of processed URL"
});

export const getProcessedUrlCounter = () => urlProcessedCounter;

const processingTimeHist = new Prometheus.Histogram({
    name: "muppets_snap_processing_time",
    help: "Processing time to scrap an URL"
});

export const getProcessingTimeHist = () => processingTimeHist;

const urlToProcessGauge = new Prometheus.Gauge({
    name: "muppets_sqs_url_to_process_gauge",
    help: "Approximative number in the input queue"
});

// Helper to compute metrics

function getSqsMetrics(sqs: AWS.SQS, queueUrl: string) {
    sqs.getQueueAttributes({QueueUrl: queueUrl, AttributeNames: ["ApproximateNumberOfMessages"]})
        .promise()
        .then(data => urlToProcessGauge.set(parseInt(data.Attributes!.ApproximateNumberOfMessages)))
        .catch(() => console.error("Cannot retrieve SQS metrics !"));
}

export function sqsMetrics(sqs: AWS.SQS, queueUrl: string) {
    setInterval(() => getSqsMetrics(sqs, queueUrl), 1000);
}
