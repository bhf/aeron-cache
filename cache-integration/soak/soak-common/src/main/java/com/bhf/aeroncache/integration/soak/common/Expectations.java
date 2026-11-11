package com.bhf.aeroncache.integration.soak.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The events a subscription is expected to receive in a round, accumulated per cache in the order the workload
 * applies the mutations that produce them. Wraps the per-cache lists so the round logic reads as a sequence of
 * {@code add(...)} calls and the verifier can ask for the whole expectation and its total size.
 */
final class Expectations {

    private final Map<String, List<ExpectedEvent>> byCache = new LinkedHashMap<>();

    Expectations(Collection<String> caches) {
        caches.forEach(c -> byCache.put(c, new ArrayList<>()));
    }

    void add(String cache, ExpectedEvent event) {
        byCache.get(cache).add(event);
    }

    Set<String> caches() {
        return byCache.keySet();
    }

    List<ExpectedEvent> forCache(String cache) {
        return byCache.getOrDefault(cache, List.of());
    }

    int total() {
        return byCache.values().stream().mapToInt(List::size).sum();
    }
}
