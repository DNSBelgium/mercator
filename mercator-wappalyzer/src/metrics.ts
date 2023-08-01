import Prometheus from "prom-client";
import { config } from './config'
import express from "express";

Prometheus.collectDefaultMetrics({ prefix: "wappalyzer_" });

export function initMetricsServer() {
    const app = express();
    app.get("/health", (_: express.Request, res: express.Response) => {
        res.send("OK"); // TODO: A pull request to wappalyzer for monitoring the health of the browser
    });
    app.get("/actuator/prometheus", async (_: express.Request, res: express.Response) => {
        res.set("Content-Type", getContentType());
        res.send(await getMetrics());
    });
    app.listen(config.server_port);
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

export { getContentType, getMetrics, getProcessedUrlCounter, getProcessingTimeHist };
