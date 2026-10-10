package be.dnsbelgium.mercator.pipeline.service;

import java.util.Collection;
import java.util.List;

public interface ItemSource <T> extends AutoCloseable {

    List<T> getItems();

    /**
     * An ItemSource that processes a static file (e.g. a CSV file) should
     * return false before the first call to getItems() and return true after the first call to getItems().
     * <p>
     * An ItemSource that processes a dynamic source (e.g. a database table) should always return false.
     *
     * @return true if the ItemSource has finished processing all items, false otherwise.
     */
    boolean isDone();

    default boolean sleepBetweenPolls() {
        return true;
    }

    /**
     * Marks the given items as fully processed, i.e. their results are durably stored. Sources
     * that track per-item state (e.g. a database work queue) use this to close their leases;
     * the default does nothing (e.g. a CSV file has no per-item state).
     *
     * <p>Called from the <b>writer thread</b> and possibly <b>after {@link #close()}</b> (the
     * producer closes the source as soon as it is drained, before the writer has flushed its
     * last batch), so implementations must not depend on the source's polling state.
     *
     * @param itemIds ids of exactly the items whose results were just persisted
     * @return the number of items the source actually marked as processed
     */
    default int acknowledge(Collection<String> itemIds) {
        return 0;
    }

    /**
     * Closes the ItemSource and releases any resources associated with it.
     */
    default void close() {
    }
}
