//! Module to limit the amount of logs that we produce

let counter = 0;
// TODO: make configurable
const MESSAGE_TRUNCATED_THRESHOLD = 10_000;

function error(...message) {
    // Always log errors
    console.error(...message);
}

function info(...message) {
    ++counter;
    if (counter >= MESSAGE_TRUNCATED_THRESHOLD)
        console.info(...message);
}

function log(...message) {
    ++counter;
    if (counter >= MESSAGE_TRUNCATED_THRESHOLD)
        console.log(...message);
}

/// Called when a domain has been crawled successfully
function log_success(domain_name) {
    ++counter;
    if (counter >= MESSAGE_TRUNCATED_THRESHOLD) {
        counter = 0;
        console.log(`Successfull screenshotted ${MESSAGE_TRUNCATED_THRESHOLD} domains, last one being ${domain_name}`);
    }
}

function log_failure(domain_name, reason) {
    ++counter;
    if (counter >= MESSAGE_TRUNCATED_THRESHOLD) {
        counter = 0;
        console.info(`Failure screenshotting ${domain_name}: ${reason}`);
    }
}

export { error, info, log, log_success };
