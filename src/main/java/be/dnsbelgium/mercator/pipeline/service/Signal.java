package be.dnsbelgium.mercator.pipeline.service;

/**
 * Typed envelope placed on the internal pipeline queues. It lets a poison pill flow
 * through a {@code BlockingQueue<Signal>} alongside real payloads without resorting
 * to sentinel {@code String}s or nullable casts (and {@link java.util.concurrent.ArrayBlockingQueue}
 * forbids {@code null} elements anyway).
 *
 * <p>The hierarchy is {@code sealed}, so consumers can exhaustively pattern-match on
 * {@link Payload} vs {@link Poison}.
 *
 * @param <T> the payload type carried by this queue
 */
@SuppressWarnings("unused")
public sealed interface Signal<T> permits Signal.Payload, Signal.Poison {

    /** A real item to be processed or written. */
    record Payload<T>(T value) implements Signal<T> { }

    /**
     * Shutdown marker. Compared structurally (by type), so any {@code Poison} instance
     * is interchangeable — a consumer can re-insert the pill it received for its siblings.
     */
    record Poison<T>() implements Signal<T> { }

    static <T> Poison<T> poison() {
        return new Poison<>();
    }

    static <T> Payload<T> payload(T value) {
        return new Payload<>(value);
    }

}

