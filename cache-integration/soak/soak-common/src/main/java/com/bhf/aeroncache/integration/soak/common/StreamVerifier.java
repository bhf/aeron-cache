package com.bhf.aeroncache.integration.soak.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.LockSupport;

/**
 * Collects and verifies the events a {@link SoakStreamSubscriber} receives against the round's {@link
 * Expectations}. Every collection is a two-sided assertion: it waits for exactly the expected number of events
 * (failing on timeout) and then grace-polls to prove no <em>extra</em> event arrives. Hydration replays are
 * compared per cache as an unordered multiset (snapshot order is not guaranteed); live streams are compared in
 * order, position by position.
 */
final class StreamVerifier {

    private static final long PARK_NANOS = 50_000L;
    private static final long GRACE_NANOS = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(300);

    private final GatewayCommandDriver driver;
    private final SoakFailures failures;
    private final SoakReport report;
    private final long opTimeoutNanos;
    private final int opTimeoutSeconds;

    StreamVerifier(GatewayCommandDriver driver, SoakFailures failures, SoakReport report,
                   long opTimeoutNanos, int opTimeoutSeconds) {
        this.driver = driver;
        this.failures = failures;
        this.report = report;
        this.opTimeoutNanos = opTimeoutNanos;
        this.opTimeoutSeconds = opTimeoutSeconds;
    }

    void verifyHydration(SoakStreamSubscriber subscriber, Expectations expected) {
        var got = collect(subscriber, expected.caches(), expected.total(), "hydration");
        // Snapshot replay order is not guaranteed, so compare per cache as an unordered multiset.
        for (var cache : expected.caches()) {
            var expectedSigs = new TreeMap<String, Integer>();
            expected.forCache(cache).forEach(ev -> expectedSigs.merge(ev.sig(), 1, Integer::sum));
            var gotSigs = new TreeMap<String, Integer>();
            got.getOrDefault(cache, List.of()).forEach(u -> gotSigs.merge(sig(u), 1, Integer::sum));
            if (!expectedSigs.equals(gotSigs)) {
                throw failures.fail("hydration mismatch for " + cache + ": expected " + expectedSigs + " got " + gotSigs);
            }
        }
        report.recordVerification();
    }

    void verifyLiveOrdered(SoakStreamSubscriber subscriber, Expectations expected) {
        var got = collect(subscriber, expected.caches(), expected.total(), "live stream");
        for (var cache : expected.caches()) {
            var exp = expected.forCache(cache);
            var rec = got.getOrDefault(cache, List.of());
            if (exp.size() != rec.size()) {
                throw failures.fail("stream event count mismatch for " + cache + ": expected " + exp.size()
                        + " " + exp + " but received " + rec.size() + " " + render(rec));
            }
            for (int i = 0; i < exp.size(); i++) {
                if (!exp.get(i).matches(rec.get(i))) {
                    throw failures.fail("stream event mismatch for " + cache + " at index " + i
                            + ": expected " + exp.get(i) + " but received " + render(List.of(rec.get(i))));
                }
            }
        }
    }

    /**
     * Collects exactly {@code totalExpected} stream events for the subscription across the given caches, then
     * grace-polls to prove no <em>extra</em> event arrives (the negative half of the assertion). Events for
     * other caches are residual/foreign and ignored (each round uses freshly created unique caches).
     */
    private Map<String, List<StreamEvent>> collect(SoakStreamSubscriber subscriber, Set<String> caches,
                                                   int totalExpected, String what) {
        var got = new HashMap<String, List<StreamEvent>>();
        caches.forEach(c -> got.put(c, new ArrayList<>()));
        int count = 0;
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (count < totalExpected) {
            var u = subscriber.poll();
            if (u == null) {
                checkHealth(subscriber);
                if (System.nanoTime() > deadline) {
                    throw failures.fail("timed out after " + opTimeoutSeconds + "s awaiting " + what
                            + " events: got " + count + "/" + totalExpected + " " + renderAll(got));
                }
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            var bucket = got.get(u.cacheId());
            if (bucket == null) {
                continue;
            }
            bucket.add(u);
            count++;
        }
        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = subscriber.poll();
            if (u == null) {
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            if (got.containsKey(u.cacheId())) {
                throw failures.fail(what + ": unexpected extra stream event after collecting " + count + "/" + totalExpected
                        + " " + renderAll(got) + ": " + u);
            }
        }
        return got;
    }

    /** Grace-polls the subscription and fails if any event arrives for one of the given caches. */
    void assertSilence(SoakStreamSubscriber subscriber, Set<String> caches, String what) {
        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = subscriber.poll();
            if (u == null) {
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            if (caches.contains(u.cacheId())) {
                throw failures.fail(what + ": " + u);
            }
        }
    }

    private void checkHealth(SoakStreamSubscriber subscriber) {
        driver.checkHealth();
        if (!subscriber.isHealthy()) {
            throw failures.fail("streaming subscriber connection failed during soak run");
        }
    }

    private static String sig(StreamEvent u) {
        return switch (u.eventType()) {
            case StreamEvent.ADD_ITEM, StreamEvent.PATCH_ITEM -> u.eventType() + "|" + u.key() + "|" + u.value();
            case StreamEvent.REMOVE_ITEM -> u.eventType() + "|" + u.key() + "|";
            default -> u.eventType() + "||";
        };
    }

    private static String render(List<StreamEvent> updates) {
        var sb = new StringBuilder("[");
        for (var u : updates) {
            sb.append(u.eventType()).append('(').append(u.key()).append('=').append(u.value()).append(") ");
        }
        return sb.append(']').toString();
    }

    private static String renderAll(Map<String, List<StreamEvent>> got) {
        var sb = new StringBuilder();
        got.forEach((k, v) -> sb.append(k).append("->").append(render(v)).append(' '));
        return sb.toString();
    }
}
