package com.bhf.aeroncache.integration.soak.common;

import java.util.List;

/**
 * Describes one subscription the {@link UrlStreamingSoakWorkload} wants to open, in transport-neutral terms.
 * A {@link SoakStreamSubscriberFactory} turns it into a concrete connection (a websocket or an SSE stream),
 * encoding scope/mode/hydration/cardinality/flavour into the subscription URL.
 *
 * @param cacheIds  the caches to subscribe to (one for single-cardinality, several for multi).
 * @param keyScoped whether the subscription is filtered to a single key ({@link #key}) rather than whole-cache.
 * @param key       the key filter when {@link #keyScoped}; otherwise ignored.
 * @param patch     whether to request patch mode (cache flavour only; counters coerce it to full).
 * @param hydrate   whether to request a snapshot on subscribe.
 * @param counters  whether this targets the counters flavour rather than regular caches.
 */
public record SubscriptionSpec(List<String> cacheIds, boolean keyScoped, String key,
                               boolean patch, boolean hydrate, boolean counters) {

    public boolean multi() {
        return cacheIds.size() > 1;
    }
}
