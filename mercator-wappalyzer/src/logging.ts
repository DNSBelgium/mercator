//! Module to limit the amount of logs that we produce

// TODO: make configurable
const DEBUG_LOGGING = false;
/// Show messages for every xth successfull domain.
const TRUNCATED_DOMAIN_COUNT = 10_000;
const TRUNCATED_FAILURE_COUNT = 500;

let domain_counter = 0;
let failure_counter = 0;

/**
 * Logs the given error message
 * Should be used for general errors (not visit-specific) and not for e.g. failures of one visit.
 */
function error(...message: any[]) {
    // Always log errors
    console.error(...message);
}

const should_be_logging = () => domain_counter >= TRUNCATED_DOMAIN_COUNT;

function visit_error(...message: any[]) {
    if (should_be_logging())
        console.error(...message);
}

function info(...message: any[]) {
    console.info(...message);
}

function visit_info(...message: any[]) {
    if (should_be_logging())
        console.info(...message);
}

/**
 * Logs all messages through internal console mechanisms in JavaScript.
 * Should be used for generic log messages, such as configuration, and not for visit related messages.
 */
function log(...message: any[]) {
    console.log(...message);
}

function visit_debug(...message: any[]) {
    if (DEBUG_LOGGING && should_be_logging())
        console.debug(...message);
}

function debug(...message: any[]) {
    if (DEBUG_LOGGING)
        console.debug(...message);
}

/// Called when a domain has been crawled successfully
function log_success(domain_name: string) {
    ++domain_counter;
    if (domain_counter >= TRUNCATED_DOMAIN_COUNT) {
        domain_counter = 0;
        console.log(`Successfull screenshotted ${TRUNCATED_DOMAIN_COUNT} domains, last one being ${domain_name}`);
    }
}

function log_failure(domain_name: string, reason: any) {
    ++failure_counter;
    if (failure_counter >= TRUNCATED_FAILURE_COUNT) {
        failure_counter = 0;
        console.info(`Failure screenshotting ${domain_name} and another ${TRUNCATED_FAILURE_COUNT - 1} domains. Reason for the first domain: ${reason}`);
    }
}

export { error, visit_error, info, visit_info, log, visit_debug, debug, log_success, log_failure };
