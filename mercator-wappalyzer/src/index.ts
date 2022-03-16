import {start_sqs_polling} from "./server";
import {init_wappalyzer} from "./wappalyzer_wrapper";
import {initMetricsServer} from "./metrics";
import {config} from './config'

async function main() {
  console.log(config);
  initMetricsServer();

  await init_wappalyzer();
  await start_sqs_polling(config.sqs_endpoint, config.sqs_input_queue, config.sqs_output_queue)
}

(async () => {
  await main().catch(err => {
    console.error("getting error", err);
    process.exit(1);
  })
})()
