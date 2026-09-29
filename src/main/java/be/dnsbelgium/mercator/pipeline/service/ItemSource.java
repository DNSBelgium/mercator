package be.dnsbelgium.mercator.pipeline.service;

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
     * Closes the ItemSource and releases any resources associated with it.
     */
    default void close() {
    }
}
