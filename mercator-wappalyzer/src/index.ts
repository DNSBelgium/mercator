import { start_sqs_polling } from "./server";
import { initMetricsServer } from "./metrics";
import { config } from './config'


import { error, log } from "./logging";

async function main() {
  initMetricsServer();

  start_sqs_polling(config.sqs_endpoint, config.sqs_input_queue, config.sqs_output_queue);
}

(async () => {
  log("Starting wappalyzer");
  await main().catch(err => {
    error("getting error", err);
    process.exit(1);
  })
})()
