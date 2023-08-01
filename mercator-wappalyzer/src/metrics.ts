import Prometheus from "prom-client";
import { config } from './config'
import express from "express";

Prometheus.collectDefaultMetrics({ prefix: "wappalyzer_" });

async function getFailureRate(): Promise<number> {
    // Errors only take into account how many crashes we got. No error if the page doesn't exist.
    const errors = await getValueOfMetric(getUrlFailed());
    const successful = await getValueOfMetric(getProcessedUrlCounter());
    const total = errors + successful;

    const failureRate = total !== 0 ? errors / total : 0.0;
    return failureRate;
}

export function initMetricsServer() {
    const app = express();
    app.get("/health", async (_: express.Request, res: express.Response) => {
        // TODO: add browser status check
        const failureRate = await getFailureRate();

        const FAILURE_THRESHOLD = config.failure_threshold;
        const health: boolean = failureRate < FAILURE_THRESHOLD;
        if (!health)
            res.status(500);
        res.send({ healthy: health, failureRate: failureRate });
    });
    app.get("/actuator/prometheus", async (_: express.Request, res: express.Response) => {
        res.set("Content-Type", getContentType());
        res.send(await getMetrics());
    });
    app.listen(config.server_port);
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

// Metrics type
const urlProcessedCounter = new Prometheus.Counter({
    name: "wappalyzer_url_processed_counter",
    help: "Number of processed URL"
});

function getProcessedUrlCounter() {
    return urlProcessedCounter;
}

const processingTimeHist = new Prometheus.Histogram({
    name: "wappalyzer_processing_time",
    help: "Processing time to wappalyze an URL"
});

function getProcessingTimeHist() {
    return processingTimeHist;
}

const urlFailed = new Prometheus.Counter({
    name: "wappalyzer_url_failed",
    help: "Number of URLs that failed processing"
});

function getUrlFailed() {
    return urlFailed;
}

export { getMetrics, getProcessedUrlCounter, getProcessingTimeHist, getUrlFailed };
