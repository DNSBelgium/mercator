import { config } from "./config";

import { spawn } from "child_process";

const DEFAULT_OPTIONS = {
    delay: config.wappalyzing_timeout,
    maxDepth: 3,
    maxUrls: 5,
    recursive: true
};

interface GoWapWrapperOptions {
}

class GoWapWrapper {
    private options: GoWapWrapperOptions = {};

    constructor(options: GoWapWrapperOptions) {
        this.options = options;
    }

    /**
     * 
     * @param url The url to parse
     */
    public async analyze(url: string) /*: GoWapResult*/ {
        // TODO
        const args = [];
        // TODO
        if (this.options) { }
        args.push(url);
        console.log("Starting gowap");
        const childProcess = spawn("./gowap", args);
        // Debug logging
        childProcess.stderr.pipe(process.stderr);

        const exitCode = await new Promise((resolve) => {
            childProcess.on('close', resolve);
        });
        console.log("exitcode was", exitCode);

        let complete_data = "";
        console.log("Waiting for gowap");
        for await (const data of childProcess.stdout) {
            complete_data = complete_data.concat(data);
        }
        console.log("Done: {}", complete_data);

        return JSON.parse(complete_data);
    }

    /**
     * Returns whether the instance is healthy.
     */
    public health(): boolean {
        // TODO: add extra checks?
        return true;
    }
}

let INSTANCE: null | GoWapWrapper = null;

async function analyze(url: string) {
    if (INSTANCE === null) {
        INSTANCE = new GoWapWrapper(DEFAULT_OPTIONS);
    }

    return INSTANCE.analyze(url);
}

async function isHealthy() {
    if (INSTANCE === null)
        return true;

    return INSTANCE.health();
}

export { GoWapWrapper, GoWapWrapperOptions, analyze, isHealthy };
