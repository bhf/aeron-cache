package com.bhf.aeroncache.integration.soak;

import java.util.HashMap;
import java.util.Map;

/**
 * The correctness oracle: an in-memory model of what the cache cluster <em>should</em> contain, kept in
 * lock-step with every mutation the workload applies. Because the workload is single-writer and every
 * mutation is applied to the model and the cluster together, the model is the authoritative expected
 * state. Read-backs and the periodic/final reconciliation sweeps compare the cluster against it.
 *
 * <p>Values are modelled exactly as the gateway returns them over the wire: key-value entries as the
 * stored string, counters as their {@code long} value (compared against the string-encoded number the
 * gateway marshals back).
 */
final class SoakOracle {

    /** cacheId -> (key -> value) for key-value caches. */
    private final Map<String, Map<String, String>> kv = new HashMap<>();
    /** cacheId -> (key -> value) for counter caches. */
    private final Map<String, Map<String, Long>> counters = new HashMap<>();

    void registerKvCache(String cacheId) {
        kv.computeIfAbsent(cacheId, k -> new HashMap<>());
    }

    void registerCounterCache(String cacheId) {
        counters.computeIfAbsent(cacheId, k -> new HashMap<>());
    }

    // ---------------------------------------------------------------- key-value model

    void kvPut(String cacheId, String key, String value) {
        kv.get(cacheId).put(key, value);
    }

    void kvRemove(String cacheId, String key) {
        kv.get(cacheId).remove(key);
    }

    void kvClear(String cacheId) {
        kv.get(cacheId).clear();
    }

    /** @return the modelled value for a key, or {@code null} if absent. */
    String kvGet(String cacheId, String key) {
        return kv.get(cacheId).get(key);
    }

    boolean kvContains(String cacheId, String key) {
        return kv.get(cacheId).containsKey(key);
    }

    Map<String, String> kvSnapshot(String cacheId) {
        return kv.get(cacheId);
    }

    // ---------------------------------------------------------------- counter model

    boolean counterContains(String cacheId, String key) {
        return counters.get(cacheId).containsKey(key);
    }

    long counterValue(String cacheId, String key) {
        return counters.get(cacheId).get(key);
    }

    void counterPut(String cacheId, String key, long value) {
        counters.get(cacheId).put(key, value);
    }

    Map<String, Long> counterSnapshot(String cacheId) {
        return counters.get(cacheId);
    }
}
