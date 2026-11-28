package be.dnsbelgium.mercator.pipeline.service;

public interface ItemSourceFactory<T> {

    T create(String moduleName);

    /**
     * Cheap check whether a pass of the module could find work. Lets the runner skip an empty
     * module without building its pipeline. Defaults to {@code true} (always run), which is right
     * for one-shot sources such as a CSV file.
     */
    default boolean hasPendingWork(String moduleName) {
        return true;
    }
}
