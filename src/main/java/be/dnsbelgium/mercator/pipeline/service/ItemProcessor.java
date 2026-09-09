package be.dnsbelgium.mercator.pipeline.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Processes one input item into one output item on the pipeline's worker threads.
 *
 * <p>Formerly this interface extended Spring Batch's {@code ItemProcessor}; that dependency has
 * been removed. {@link #processItem(Object)} is the single abstract method (so implementations can
 * be supplied as a lambda / method reference), while {@link #process(Object)} is kept as a
 * convenience alias that some crawlers override directly.
 */
public interface ItemProcessor<Input, Output> {

     /** Convenience alias; by default delegates to {@link #processItem(Object)}. */
     default @Nullable Output process(@NonNull Input item) throws Exception {
          return processItem(item);
     }

     @Nullable Output processItem(@NonNull Input item);

}
