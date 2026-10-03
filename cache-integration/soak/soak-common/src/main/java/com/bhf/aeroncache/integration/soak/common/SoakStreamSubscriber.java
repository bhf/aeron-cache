package com.bhf.aeroncache.integration.soak.common;

/**
 * A soak-grade observer of a single streaming subscription over a URL-based transport (streaming WebSocket or
 * SSE). It is opened by a {@link SoakStreamSubscriberFactory} already subscribed, buffers every received
 * update into a bounded queue the workload drains via {@link #poll()}, and is closed to unsubscribe.
 *
 * <p>Unlike the integration-test streaming helpers (which open a connection, collect a fixed count and close),
 * this stays open across a round's mutation sequence so the workload can verify ordered live events, snapshot
 * hydration and silence-after-unsubscribe over an arbitrarily long run without the buffer growing unbounded
 * (the workload removes each update as it consumes it).
 */
public interface SoakStreamSubscriber extends AutoCloseable {

    /** Blocks until the subscription ack is observed; throws {@link IllegalStateException} on timeout. */
    void awaitAck(long timeoutNanos);

    /** Removes and returns the next buffered update, or {@code null} if none is currently buffered. */
    StreamEvent poll();

    /** Whether the underlying connection is still healthy (no failure/unexpected close observed). */
    boolean isHealthy();

    /** Closes the connection (i.e. unsubscribes). */
    @Override
    void close();
}
