package com.bhf.aeroncache.integration.soak;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import io.aeron.Aeron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

/**
 * The single-writer, time-bounded soak workload.
 *
 * <p>One thread drives a weighted, seeded-random mix of core key-value and counter operations against a
 * bounded key space spread over a handful of caches, keeping a {@link SoakOracle} model in lock-step with
 * every applied mutation. Each operation is verified immediately where the gateway echoes a value (gets,
 * counter inc/dec/set), and the full model is reconciled against the cluster every
 * {@link SoakConfig#verifyEvery} operations and once more at the end. Any divergence - or a gateway
 * disconnect, error, or stalled operation - fails the run with a reproducible detail (the seed is logged).
 *
 * <p>Because the key space is bounded, the data held by both the cluster and the oracle is bounded too, so
 * heap that grows with <em>time</em> rather than with the key space is a genuine leak - which is the point.
 */
final class SoakWorkload {

    /** The operations the workload issues; weights and applicability are resolved in {@link #buildOpTable}. */
    enum OpType {
        KV_PUT,
        KV_GET,
        KV_REMOVE,
        KV_CLEAR,
        KV_DELETE_RECREATE,
        COUNTER_INC,
        COUNTER_DEC,
        COUNTER_SET,
        COUNTER_GET
    }

    private static final Logger log = LogManager.getLogger(SoakWorkload.class);

    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;
    private static final long PROGRESS_EVERY = 5_000L;
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final GatewayClient client;
    private final SoakRecordingListener listener;
    private final SoakConfig cfg;
    private final SoakReport report;
    private final SoakOracle oracle = new SoakOracle();

    private final Random rnd;
    private final long opTimeoutNanos;
    private final OpType[] opTable;

    private final List<String> kvCacheIds = new ArrayList<>();
    private final List<String> counterCacheIds = new ArrayList<>();

    private long corrSeq;

    SoakWorkload(GatewayClient client, SoakRecordingListener listener, SoakConfig cfg, SoakReport report) {
        this.client = client;
        this.listener = listener;
        this.cfg = cfg;
        this.report = report;
        this.rnd = new Random(cfg.seed);
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.opTable = buildOpTable(cfg);
    }

    void run() {
        log.info("Starting soak run with {}", cfg);
        setupCaches();

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.durationSeconds);
        while (System.nanoTime() < deadline) {
            final OpType op = opTable[rnd.nextInt(opTable.length)];
            execute(op);
            report.recordOp(op);

            final long ops = report.totalOps();
            if (ops % cfg.verifyEvery == 0) {
                reconcileAll();
            }
            if (ops % PROGRESS_EVERY == 0) {
                report.sampleHeap();
                checkHealth();
                log.info(report.progressLine());
            }
        }

        // Final full reconciliation so the run ends on a verified-consistent state.
        reconcileAll();
        report.sampleHeap();
        log.info("Soak run complete: {}", report.progressLine());
    }

    // ---------------------------------------------------------------- setup

    private void setupCaches() {
        for (int i = 0; i < cfg.kvCacheCount; i++) {
            var id = "soak-kv-" + i;
            createCache(id);
            oracle.registerKvCache(id);
            kvCacheIds.add(id);
        }
        for (int i = 0; i < cfg.counterCacheCount; i++) {
            var id = "soak-ctr-" + i;
            createCounterCache(id);
            oracle.registerCounterCache(id);
            counterCacheIds.add(id);
        }
        log.info("Created {} kv caches and {} counter caches", kvCacheIds.size(), counterCacheIds.size());
    }

    // ---------------------------------------------------------------- operation dispatch

    private void execute(OpType op) {
        switch (op) {
            case KV_PUT -> kvPut();
            case KV_GET -> kvGet();
            case KV_REMOVE -> kvRemove();
            case KV_CLEAR -> kvClear();
            case KV_DELETE_RECREATE -> kvDeleteRecreate();
            case COUNTER_INC -> counterIncrement();
            case COUNTER_DEC -> counterDecrement();
            case COUNTER_SET -> counterSet();
            case COUNTER_GET -> counterGet();
        }
    }

    private void kvPut() {
        var cache = randomKvCache();
        var key = randomKey();
        var value = randomValue();
        var corr = nextCorr();
        sendWithRetry(() -> client.addEntry(corr, cache, key, value, TTL_NONE));
        expectSuccess(awaitCommand(corr), "addEntry " + cache + "/" + key);
        oracle.kvPut(cache, key, value);
    }

    private void kvGet() {
        var cache = randomKvCache();
        var key = randomKey();
        var corr = nextCorr();
        sendWithRetry(() -> client.getEntry(corr, cache, key));
        var response = awaitCommand(corr);
        var expected = oracle.kvGet(cache, key);
        if (expected == null) {
            if (response.status() != OperationStatus.UNKNOWN_KEY) {
                throw fail("getEntry " + cache + "/" + key + " expected UNKNOWN_KEY (modelled absent) but got "
                        + response.status() + " value='" + response.value() + "'");
            }
        } else {
            expectSuccess(response, "getEntry " + cache + "/" + key);
            if (!expected.equals(response.value())) {
                throw fail("getEntry " + cache + "/" + key + " expected '" + expected
                        + "' but cluster returned '" + response.value() + "'");
            }
        }
    }

    private void kvRemove() {
        var cache = randomKvCache();
        var key = randomKey();
        var corr = nextCorr();
        sendWithRetry(() -> client.removeEntry(corr, cache, key));
        var response = awaitCommand(corr);
        // The key may or may not have been present; both outcomes are valid for a blind remove.
        if (response.status() != OperationStatus.SUCCESS && response.status() != OperationStatus.UNKNOWN_KEY) {
            throw fail("removeEntry " + cache + "/" + key + " unexpected status " + response.status());
        }
        oracle.kvRemove(cache, key);
    }

    private void kvClear() {
        var cache = randomKvCache();
        var corr = nextCorr();
        sendWithRetry(() -> client.clearCache(corr, cache));
        expectSuccess(awaitCommand(corr), "clearCache " + cache);
        oracle.kvClear(cache);
    }

    private void kvDeleteRecreate() {
        var cache = randomKvCache();
        var deleteCorr = nextCorr();
        sendWithRetry(() -> client.deleteCache(deleteCorr, cache));
        expectSuccess(awaitCommand(deleteCorr), "deleteCache " + cache);

        var createCorr = nextCorr();
        sendWithRetry(() -> client.createCache(createCorr, cache));
        expectSuccess(awaitCommand(createCorr), "recreateCache " + cache);
        oracle.kvClear(cache);
    }

    private void counterIncrement() {
        var cache = randomCounterCache();
        var key = randomKey();
        ensureCounter(cache, key);
        var delta = (long) (rnd.nextInt(10) + 1);
        var expected = oracle.counterValue(cache, key) + delta;
        var corr = nextCorr();
        sendWithRetry(() -> client.incrementCounter(corr, cache, key, delta, TTL_NONE));
        var response = awaitCommand(corr);
        expectSuccess(response, "incrementCounter " + cache + "/" + key);
        assertCounterValue(response.value(), expected, "incrementCounter " + cache + "/" + key);
        oracle.counterPut(cache, key, expected);
    }

    private void counterDecrement() {
        var cache = randomCounterCache();
        var key = randomKey();
        ensureCounter(cache, key);
        var delta = (long) (rnd.nextInt(10) + 1);
        var expected = oracle.counterValue(cache, key) - delta;
        var corr = nextCorr();
        sendWithRetry(() -> client.decrementCounter(corr, cache, key, delta, TTL_NONE));
        var response = awaitCommand(corr);
        expectSuccess(response, "decrementCounter " + cache + "/" + key);
        assertCounterValue(response.value(), expected, "decrementCounter " + cache + "/" + key);
        oracle.counterPut(cache, key, expected);
    }

    private void counterSet() {
        var cache = randomCounterCache();
        var key = randomKey();
        ensureCounter(cache, key);
        var value = (long) rnd.nextInt(1_000_000);
        var corr = nextCorr();
        sendWithRetry(() -> client.setCounter(corr, cache, key, value, TTL_NONE));
        var response = awaitCommand(corr);
        expectSuccess(response, "setCounter " + cache + "/" + key);
        assertCounterValue(response.value(), value, "setCounter " + cache + "/" + key);
        oracle.counterPut(cache, key, value);
    }

    private void counterGet() {
        var cache = randomCounterCache();
        var key = randomKey();
        var corr = nextCorr();
        sendWithRetry(() -> client.getCounterEntry(corr, cache, key));
        var response = awaitCommand(corr);
        if (oracle.counterContains(cache, key)) {
            expectSuccess(response, "getCounterEntry " + cache + "/" + key);
            assertCounterValue(response.value(), oracle.counterValue(cache, key), "getCounterEntry " + cache + "/" + key);
        } else if (response.status() != OperationStatus.UNKNOWN_KEY) {
            throw fail("getCounterEntry " + cache + "/" + key + " expected UNKNOWN_KEY (modelled absent) but got "
                    + response.status() + " value='" + response.value() + "'");
        }
    }

    /** Lazily creates a counter entry (initialised to 0) so inc/dec/set/get have a defined starting point. */
    private void ensureCounter(String cache, String key) {
        if (oracle.counterContains(cache, key)) {
            return;
        }
        var corr = nextCorr();
        sendWithRetry(() -> client.addCounterEntry(corr, cache, key, 0L, TTL_NONE));
        expectSuccess(awaitCommand(corr), "addCounterEntry " + cache + "/" + key);
        oracle.counterPut(cache, key, 0L);
    }

    // ---------------------------------------------------------------- reconciliation

    private void reconcileAll() {
        for (var cache : kvCacheIds) {
            var actual = fetchEntries(cache);
            var expected = oracle.kvSnapshot(cache);
            if (!expected.equals(actual)) {
                throw fail("KV reconciliation mismatch for " + cache + ": " + diff(expected, actual));
            }
        }
        for (var cache : counterCacheIds) {
            var actual = fetchCounterEntries(cache);
            var expected = new HashMap<String, String>();
            oracle.counterSnapshot(cache).forEach((k, v) -> expected.put(k, Long.toString(v)));
            if (!expected.equals(actual)) {
                throw fail("counter reconciliation mismatch for " + cache + ": " + diff(expected, actual));
            }
        }
        report.recordVerification();
    }

    private Map<String, String> fetchEntries(String cache) {
        var corr = nextCorr();
        sendWithRetry(() -> client.getEntries(corr, cache));
        return awaitEntries(corr, "getEntries " + cache);
    }

    private Map<String, String> fetchCounterEntries(String cache) {
        var corr = nextCorr();
        sendWithRetry(() -> client.getCounterEntries(corr, cache));
        return awaitEntries(corr, "getCounterEntries " + cache);
    }

    // ---------------------------------------------------------------- await + health (with cleanup)

    private SoakRecordingListener.CommandResponse awaitCommand(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        SoakRecordingListener.CommandResponse response;
        while ((response = listener.commandResponses.remove(corr)) == null) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting response for " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        return response;
    }

    private Map<String, String> awaitEntries(String corr, String what) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (!listener.entriesComplete.containsKey(corr)) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting " + what);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        var status = listener.entriesStatus.remove(corr);
        listener.entriesComplete.remove(corr);
        var items = listener.entriesAccumulated.remove(corr);
        if (status != OperationStatus.SUCCESS) {
            throw fail(what + " returned status " + status);
        }
        return items == null ? Map.of() : items;
    }

    private void checkHealth() {
        if (!client.isConnected()) {
            throw fail("gateway client disconnected during soak run");
        }
        if (!listener.errors.isEmpty()) {
            var error = listener.errors.entrySet().iterator().next();
            throw fail("gateway reported an error for " + error.getKey() + ": " + error.getValue());
        }
    }

    private void sendWithRetry(LongSupplier send) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (send.getAsLong() == Aeron.NULL_VALUE) {
            if (System.nanoTime() > deadline) {
                throw fail("timed out offering a request frame (sustained backpressure)");
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
    }

    // ---------------------------------------------------------------- assertions + helpers

    private void expectSuccess(SoakRecordingListener.CommandResponse response, String what) {
        if (response.status() != OperationStatus.SUCCESS) {
            throw fail(what + " expected SUCCESS but got " + response.status());
        }
    }

    private void assertCounterValue(String actual, long expected, String what) {
        if (!Long.toString(expected).equals(actual)) {
            throw fail(what + " expected " + expected + " but cluster returned '" + actual + "'");
        }
    }

    private AssertionError fail(String detail) {
        report.recordMismatch(detail);
        return new AssertionError(detail);
    }

    private String diff(Map<String, String> expected, Map<String, String> actual) {
        var sb = new StringBuilder();
        var shown = 0;
        for (var e : expected.entrySet()) {
            if (!Objects.equals(e.getValue(), actual.get(e.getKey()))) {
                sb.append("[key=").append(e.getKey()).append(" expected=").append(e.getValue())
                        .append(" actual=").append(actual.get(e.getKey())).append("] ");
                if (++shown >= 5) {
                    break;
                }
            }
        }
        for (var k : actual.keySet()) {
            if (!expected.containsKey(k)) {
                sb.append("[unexpectedKey=").append(k).append(" actual=").append(actual.get(k)).append("] ");
                if (++shown >= 10) {
                    break;
                }
            }
        }
        sb.append("(expectedSize=").append(expected.size()).append(", actualSize=").append(actual.size()).append(')');
        return sb.toString();
    }

    private String randomKvCache() {
        return kvCacheIds.get(rnd.nextInt(kvCacheIds.size()));
    }

    private String randomCounterCache() {
        return counterCacheIds.get(rnd.nextInt(counterCacheIds.size()));
    }

    private String randomKey() {
        return "k" + rnd.nextInt(cfg.keySpace);
    }

    private String randomValue() {
        var chars = new char[cfg.valueSizeBytes];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = ALPHABET[rnd.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }

    private String nextCorr() {
        return "c" + (++corrSeq);
    }

    private void createCache(String id) {
        var corr = nextCorr();
        sendWithRetry(() -> client.createCache(corr, id));
        expectSuccess(awaitCommand(corr), "createCache " + id);
    }

    private void createCounterCache(String id) {
        var corr = nextCorr();
        sendWithRetry(() -> client.createCounterCache(corr, id));
        expectSuccess(awaitCommand(corr), "createCounterCache " + id);
    }

    /**
     * Expands the (applicable) operation weights into a flat table that {@link #run()} indexes with a single
     * bounded random draw. Operations for a cache category with zero caches configured are dropped so the
     * workload never targets a category it cannot serve.
     */
    private static OpType[] buildOpTable(SoakConfig cfg) {
        var weights = new HashMap<OpType, Integer>();
        if (cfg.kvCacheCount > 0) {
            weights.put(OpType.KV_PUT, 45);
            weights.put(OpType.KV_GET, 25);
            weights.put(OpType.KV_REMOVE, 10);
            weights.put(OpType.KV_CLEAR, 2);
            weights.put(OpType.KV_DELETE_RECREATE, 1);
        }
        if (cfg.counterCacheCount > 0) {
            weights.put(OpType.COUNTER_INC, 8);
            weights.put(OpType.COUNTER_DEC, 4);
            weights.put(OpType.COUNTER_SET, 3);
            weights.put(OpType.COUNTER_GET, 2);
        }
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Soak requires at least one kv or counter cache (kvCacheCount="
                    + cfg.kvCacheCount + ", counterCacheCount=" + cfg.counterCacheCount + ")");
        }

        var table = new ArrayList<OpType>();
        weights.forEach((op, weight) -> {
            for (int i = 0; i < weight; i++) {
                table.add(op);
            }
        });
        return table.toArray(new OpType[0]);
    }
}
