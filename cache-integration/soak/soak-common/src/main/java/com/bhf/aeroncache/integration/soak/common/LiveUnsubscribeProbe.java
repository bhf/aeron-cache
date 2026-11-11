package com.bhf.aeroncache.integration.soak.common;

import java.util.List;

/**
 * An optional capability of a {@link SoakStreamSubscriber}: unsubscribe from a set of still-alive caches while
 * the observer stays able to {@link SoakStreamSubscriber#poll()} for events, so the workload can issue a
 * mutation that <em>would</em> stream and prove the subscription has fallen silent.
 *
 * <p>Only the binary Aeron transport exposes this - it can send an explicit unsubscribe and keep reading. The
 * URL transports (WS/SSE) cannot: closing the connection is the unsubscribe, and a post-close probe would race
 * the server's disconnect handling, so {@link SoakStreamSubscriber#liveUnsubscribeProbe()} returns {@code null}
 * for them and that negative check is skipped.
 */
public interface LiveUnsubscribeProbe {

    /** Unsubscribes the given caches (best-effort / fire-and-forget) and drains any buffered events, so a
     * subsequent {@link SoakStreamSubscriber#poll()} sees only events that arrive <em>after</em> the unsubscribe.
     *
     * @param cacheIds the still-alive caches to unsubscribe (deleted caches are excluded by the caller).
     * @param counters whether these are counter caches rather than regular caches. */
    void unsubscribe(List<String> cacheIds, boolean counters);
}
