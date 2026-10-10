package be.dnsbelgium.mercator.pipeline.service;

import java.util.List;

/**
 * Callback used by {@link JsonItemWriter} to report that a batch of results is now durably
 * stored in Parquet, e.g. so a work queue can mark the corresponding tasks as done
 * ({@link ItemSource#acknowledge(java.util.Collection)}).
 *
 * <p>Invoked on the writer thread, after the batch was converted and its JSON files were
 * removed. A failing listener never undoes the conversion; see {@link JsonItemWriter}.
 */
@FunctionalInterface
public interface BatchCommitListener {

    /**
     * Called after a batch was converted to Parquet.
     *
     * @param itemIds ids of exactly the items in that batch (never empty)
     * @throws RuntimeException if the commit could not be recorded; {@link JsonItemWriter}
     *                          remembers the first failure and rethrows it from {@code flush()}
     */
    void onBatchCommitted(List<String> itemIds);
}

