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
    console.log("Wappalyzer loaded");
}

export async function close() {
    await wappalyzer.destroy();
    console.log("Wappalyzer destroyed");
}

export async function wappalyze(url: string) {
    try {
        console.log("Wappalyzing " + url);
        const endProcessingTimeHist = getProcessingTimeHist().startTimer();
        const site = await wappalyzer.open(url)
        // const report = await site.analyze();
        const report = await Promise.race([
            site.analyze(),
            new Promise((_, reject) => setTimeout(() => reject(new Error('Timeout')), 300000)) // throw error after 5 min
        ]);
        getProcessedUrlCounter().inc();
        endProcessingTimeHist();
        console.log(url + " wappalyzed");
        return report;
    } catch (e) {
        console.error("Something went wrong");
        getUrlFailed().inc();
        console.error(e);
        process.exit(1); // Kill the pod to re-initiate the browser.
    }
}
