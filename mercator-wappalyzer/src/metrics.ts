import Prometheus from "prom-client";
import {config} from './config'
import express from "express";

Prometheus.collectDefaultMetrics({prefix: "wappalyzer_"});

export function initMetricsServer() {
    const app = express();
    app.get("/health", (_: any, res) => {
        res.send("OK"); // TODO: A pull request to wappalyzer for monitoring the health of the browser
    });
    app.get("/actuator/prometheus", (_: any, res) => {
        res.set("Content-Type", getContentType());
        res.send(getMetrics());
    });
    app.listen(config.server_port);
}

const getContentType = () => Prometheus.register.contentType;
const getMetrics = () => Prometheus.register.metrics();

// Metrics type
const urlProcessedCounter = new Prometheus.Counter({
    name: "wappalyzer_url_processed_counter",
    help: "Number of processed URL"
});

export const getProcessedUrlCounter = () => urlProcessedCounter;

const processingTimeHist = new Prometheus.Histogram({
    name: "wappalyzer_processing_time",
    help: "Processing time to wappalyze an URL"
});

export const getProcessingTimeHist = () => processingTimeHist;
