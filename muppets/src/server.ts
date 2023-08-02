import { createConsumer, createHandler, createProducer, getQueueUrl } from "./websnapper.js";
import { config } from "./config.js";
import express from "express";
import * as metrics from "./metrics.js";
import { isBrowserConnected, shutdown } from "./scraper.js";

let server;
let consumer;

async function gracefulStop() {
    console.log("Shutdown initiated");

    console.log("Closing Express server");
    if (server) server.close();

    console.log("Closing SQS Consumer");
    if (consumer) consumer.stop();

    await new Promise(resolve => consumer.on("stopped", resolve)).then(shutdown).catch(() => { });
}

const handle = code => {
    console.log("Received [%s]", code);

    gracefulStop().then(() => {
        console.log("Exiting now ..");
        process.exit(0);
    });
};

process.on("SIGINT", handle);
process.on("SIGTERM", handle);

(async () => {
    const [inputQueueName, outputQueueName] = await Promise.all([
        getQueueUrl(config.sqs_input_queue),
        getQueueUrl(config.sqs_output_queue),
    ]).catch(err => {
        console.log("Cannot determine queue URLs : %s", err);
        process.exit(-1);
    });

    const producer = createProducer(outputQueueName!);
    const handler = await createHandler(producer);
    consumer = createConsumer(inputQueueName!, handler);

    const app = express();
    // health for  muppets checks browser
    app.get("/health", async (req: express.Request, res: express.Response) => {

        const browserConnected = isBrowserConnected();
        const failureRate = await metrics.calculateFailureRate();
        const failureRateHealthy = failureRate < config.failure_threshold;

        const healthy = browserConnected && failureRateHealthy;

        if (!healthy)
            res.status(500);

        res.send({ healthy, failureRate, browserConnected });
    });

    app.get("/actuator/prometheus", async (req: express.Request, res: express.Response) => {
        res.set("Content-Type", metrics.getContentType());
        res.send(await metrics.getMetrics());
    });
    server = app.listen(config.server_port);

    consumer.start();
    console.log("websnapper is running: " + consumer.isRunning);

})();
