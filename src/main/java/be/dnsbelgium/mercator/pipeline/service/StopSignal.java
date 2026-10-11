package be.dnsbelgium.mercator.pipeline.service;

/**
 * Thread-safe, <b>sticky</b>, one-way "please stop" flag for a running pipeline pass.
 *
 * <p>It is owned by whoever wants to stop the work (the {@code QueueModuleRunner}) and handed to
 * the module on every pass. Once {@link #requestStop()} was called it stays requested, which
 * removes the race "stop requested after the loop checked its flag but before the pass created
 * its pipeline": a pass that starts afterwards simply sees the flag, leases nothing and returns.
 *
 * <p>"Stop" means <em>stop starting new work</em>: items already inside the processor run to
 * completion, are written, rolled up and acked; items that were leased but not started are
 * skipped (and recycled later by the lease reaper).
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public final class StopSignal {

    private static final StopSignal NEVER = new StopSignal(true);

    private final boolean neverTriggered;
    private volatile boolean stopRequested;

    public StopSignal() {
        this(false);
    }

    private StopSignal(boolean neverTriggered) {
        this.neverTriggered = neverTriggered;
    }

    /**
     * A shared instance that is never triggered; used by the stateless (CSV) flow and by call
     * sites that cannot be stopped. {@link #requestStop()} is not allowed on it.
     */
    public static StopSignal never() {
        return NEVER;
    }

    /** Requests the stop. Idempotent and callable from any thread. */
    public void requestStop() {
        if (neverTriggered) {
            throw new UnsupportedOperationException("StopSignal.never() cannot be triggered");
        }
        stopRequested = true;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }
}

