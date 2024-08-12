import { config } from "./config";
import { log_failure, log_success, visit_info } from "./logging";
import { getUrlFailed } from "./metrics";

import spawnPlease from "spawn-please";

const DEFAULT_OPTIONS = {
    delay: config.wappalyzing_timeout,
    maxDepth: 3,
    maxUrls: 5,
    recursive: true
};

interface GoWapWrapperOptions {
}

interface GoWapUrlResponse {
    url?: string;
    status: number;
    error?: string | null;
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
        visit_info("Gowapping " + url);

        //const childProcess = spawn("./gowap", args);
        const { stdout, stderr } = await spawnPlease(config.gowap_path, args, { rejectOnError: false });

        // Everything should be fine
        if (stderr.endsWith("analyzePageFailed\n")) {
            const regex = /ERRO\[0000\] Scraper failed : (?:navigation failed: )?(.+)$/gm;
            const error = regex.exec(stderr)?.at(1);
            getUrlFailed().inc()
            log_failure(url, error);
            throw error;
        }

        log_success(url);
        const parsed = JSON.parse(stdout);
        // Keep compatible with existing db scheme:
        // TODO:

        // To remain compatible with existing db schemes
        (parsed["urls"] as any[]).reduce(function (map, obj) {
            map[obj.key] = obj.val;
            return map;
        }, {});
        const urls = (parsed["urls"] as GoWapUrlResponse[]).reduce((map: any, obj: GoWapUrlResponse) => {
            map[obj.url!] = { status: obj.status, error: null };
            return map;
        }, {});

        return { urls: urls, technologies: parsed["technologies"] };
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
