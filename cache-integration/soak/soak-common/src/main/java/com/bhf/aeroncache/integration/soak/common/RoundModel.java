package com.bhf.aeroncache.integration.soak.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-round expected state of the caches under test (regular item values and counter values), keyed by a
 * {@code cache + key} composite. The workload maintains it in lock-step with the mutations it applies so it can
 * compute the exact events a subscription must receive and generate overwrite values that are guaranteed to
 * differ from the current one.
 */
final class RoundModel {

    private final Map<String, String> regular = new HashMap<>();
    private final Map<String, Long> counters = new HashMap<>();

    private static String composite(String cache, String key) {
        return cache + '\u0001' + key;
    }

    void putRegular(String cache, String key, String value) {
        regular.put(composite(cache, key), value);
    }

    void removeRegular(String cache, String key) {
        regular.remove(composite(cache, key));
    }

    boolean containsRegular(String cache, String key) {
        return regular.containsKey(composite(cache, key));
    }

    String regular(String cache, String key) {
        return regular.get(composite(cache, key));
    }

    void putCounter(String cache, String key, long value) {
        counters.put(composite(cache, key), value);
    }

    long counter(String cache, String key) {
        return counters.get(composite(cache, key));
    }

    void clear(String cache) {
        var prefix = cache + '\u0001';
        regular.keySet().removeIf(k -> k.startsWith(prefix));
        counters.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
