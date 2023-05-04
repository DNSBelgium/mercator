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

const domainTimeOuts = new Prometheus.Counter({
    name: "muppets_timeout_domains",
    help: "Amount of timeouts from this server"
});

// prometeus bucket for keeping a record of screenschot sizes
export function getScreenshotsSizes() {
    return screenshotSizes;
}

const screenshotSizes =new Prometheus.Histogram({
    name: "muppets_screenshot_sizes",
    help: "sizes of screenshots recorded during scrape"
})

// prometeus bucket for counter where the screenshot get counted that are above a certain size
export function getBigScreenshotCounter() {
    return bigScreenshotCounter;
}

const bigScreenshotCounter = new Prometheus.Counter({
    name: "muppets_largerScreenshot_Counter",
    help: "Amount screenshots bigger then 10 MiB"
    }
)

export const getDomainTimeOuts = () => domainTimeOuts;

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
