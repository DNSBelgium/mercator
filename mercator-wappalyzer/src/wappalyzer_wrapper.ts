import { config } from "./config";
import { info, log_failure, log_success, visit_info } from "./logging";
import { getProcessedUrlCounter, getProcessingTimeHist, getUrlFailed } from "./metrics";

const Wappalyzer = require('wappalyzer');

const options = {
    delay: 500,
    maxDepth: 3,
    maxUrls: 5,
    recursive: true
};
const wappalyzer = new Wappalyzer(options);

export async function init_wappalyzer() {
    await wappalyzer.init();
    info("Wappalyzer loaded");
}

export async function close() {
    await wappalyzer.destroy();
    info("Wappalyzer destroyed");
}

export async function wappalyze(url: string) {
    try {
        visit_info("Wappalyzing " + url);
        const endProcessingTimeHist = getProcessingTimeHist().startTimer();
        const site = await wappalyzer.open(url)
        // const report = await site.analyze();
        const report = await Promise.race([
            site.analyze(),
            new Promise((_, reject) => setTimeout(() => reject(new Error('Timeout')), config.wappalyzing_timeout)) // throw error after 5 min
        ]);
        getProcessedUrlCounter().inc();
        endProcessingTimeHist();
        log_success(url);
        return report;
    } catch (e) {
        log_failure(url, e);
        getUrlFailed().inc();
        process.exit(1); // Kill the pod to re-initiate the browser.
    }
}
