package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.CommandResponse;
import com.bhf.aeroncache.integration.soak.common.SoakOracle;
import com.bhf.aeroncache.integration.soak.common.SoakReport;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
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

/**
 * The command/oracle phase of the bidi soak: a single-writer, time-bounded driver that issues a weighted,
 * seeded-random mix of key-value and counter commands over the bidi socket, keeping a {@link SoakOracle} in
 * lock-step and reconciling the full model against the cluster every {@link BidiSoakConfig#verifyEvery}
 * operations (and once at the end). It is the bidi counterpart of the core-cache gateway soak - identical
 * cache surface and oracle, driven over the websocket command protocol instead of Aeron.
 *
 * <p>Because the key space is bounded, both the cluster's and the oracle's data are bounded, so heap that
 * grows with <em>time</em> is a genuine leak - the point of the soak.
 */
final class BidiCommandSoakPhase {

    enum OpType {
        KV_PUT, KV_GET, KV_REMOVE, KV_CLEAR, KV_DELETE_RECREATE,
        COUNTER_INC, COUNTER_DEC, COUNTER_SET, COUNTER_GET
    }

    private static final Logger log = LogManager.getLogger(BidiCommandSoakPhase.class);

    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;
    private static final long PROGRESS_EVERY = 5_000L;
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final String OK = "SUCCESS";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";

    private final BidiSoakClient client;
    private final BidiSoakConfig cfg;
    private final SoakReport report;
    private final SoakOracle oracle = new SoakOracle();
    private final Random rnd;
    private final long opTimeoutNanos;
    private final OpType[] opTable;

    private final List<String> kvCacheIds = new ArrayList<>();
    private final List<String> counterCacheIds = new ArrayList<>();

    private long corrSeq;

    BidiCommandSoakPhase(BidiSoakClient client, BidiSoakConfig cfg, SoakReport report, Random rnd) {
        this.client = client;
        this.cfg = cfg;
        this.report = report;
        this.rnd = rnd;
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.opTable = buildOpTable(cfg);
    }

    void run(long deadlineNanos) {
        log.info("Starting bidi command phase with {}", cfg);
        setupCaches();

        while (System.nanoTime() < deadlineNanos) {
            var op = opTable[rnd.nextInt(opTable.length)];
            execute(op);
            report.recordCount("cmd." + op.name());

            var ops = report.total();
            if (ops % cfg.verifyEvery == 0) {
                reconcileAll();
            }
            if (ops % PROGRESS_EVERY == 0) {
                report.sampleHeap();
                checkHealth();
                log.info("command phase: {}", report.progressLine());
            }
        }

        reconcileAll();
        report.sampleHeap();
        log.info("Bidi command phase complete: {}", report.progressLine());
    }

    // ---------------------------------------------------------------- setup

    private void setupCaches() {
        for (int i = 0; i < cfg.kvCacheCount; i++) {
            var id = "soak-bidi-kv-" + i;
            command(WsOp.CREATE_CACHE, id, null, null, TTL_NONE, 0, "createCache " + id);
            oracle.registerKvCache(id);
            kvCacheIds.add(id);
        }
        for (int i = 0; i < cfg.counterCacheCount; i++) {
            var id = "soak-bidi-ctr-" + i;
            command(WsOp.CREATE_COUNTER_CACHE, id, null, null, TTL_NONE, 0, "createCounterCache " + id);
            oracle.registerCounterCache(id);
            counterCacheIds.add(id);
        }
        log.info("Created {} kv caches and {} counter caches", kvCacheIds.size(), counterCacheIds.size());
    }

    // ---------------------------------------------------------------- dispatch

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
        command(WsOp.ADD_CACHE_ENTRY, cache, key, value, TTL_NONE, 0, "addEntry " + cache + "/" + key);
        oracle.kvPut(cache, key, value);
    }

    private void kvGet() {
        var cache = randomKvCache();
        var key = randomKey();
        var corr = nextCorr();
        client.command(WsOp.GET_CACHE_ENTRY, corr, cache, key, null, TTL_NONE, 0);
        var response = awaitCommand(corr);
        var expected = oracle.kvGet(cache, key);
        if (expected == null) {
            if (!UNKNOWN_KEY.equals(response.status())) {
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
        client.command(WsOp.REMOVE_CACHE_ENTRY, corr, cache, key, null, TTL_NONE, 0);
        var response = awaitCommand(corr);
        if (!OK.equals(response.status()) && !UNKNOWN_KEY.equals(response.status())) {
            throw fail("removeEntry " + cache + "/" + key + " unexpected status " + response.status());
        }
        oracle.kvRemove(cache, key);
    }

    private void kvClear() {
        var cache = randomKvCache();
        command(WsOp.CLEAR_CACHE, cache, null, null, TTL_NONE, 0, "clearCache " + cache);
        oracle.kvClear(cache);
    }

    private void kvDeleteRecreate() {
        var cache = randomKvCache();
        command(WsOp.DELETE_CACHE, cache, null, null, TTL_NONE, 0, "deleteCache " + cache);
        command(WsOp.CREATE_CACHE, cache, null, null, TTL_NONE, 0, "recreateCache " + cache);
        oracle.kvClear(cache);
    }

    private void counterIncrement() {
        var cache = randomCounterCache();
        var key = randomKey();
        ensureCounter(cache, key);
        var delta = (long) (rnd.nextInt(10) + 1);
        var expected = oracle.counterValue(cache, key) + delta;
        var corr = nextCorr();
        client.command(WsOp.INCREMENT_COUNTER_ENTRY, corr, cache, key, null, TTL_NONE, delta);
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
        client.command(WsOp.DECREMENT_COUNTER_ENTRY, corr, cache, key, null, TTL_NONE, delta);
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
        client.command(WsOp.SET_COUNTER_ENTRY, corr, cache, key, null, TTL_NONE, value);
        var response = awaitCommand(corr);
        expectSuccess(response, "setCounter " + cache + "/" + key);
        assertCounterValue(response.value(), value, "setCounter " + cache + "/" + key);
        oracle.counterPut(cache, key, value);
    }

    private void counterGet() {
        var cache = randomCounterCache();
        var key = randomKey();
        var corr = nextCorr();
        client.command(WsOp.GET_COUNTER_ENTRY, corr, cache, key, null, TTL_NONE, 0);
        var response = awaitCommand(corr);
        if (oracle.counterContains(cache, key)) {
            expectSuccess(response, "getCounterEntry " + cache + "/" + key);
            assertCounterValue(response.value(), oracle.counterValue(cache, key), "getCounterEntry " + cache + "/" + key);
        } else if (!UNKNOWN_KEY.equals(response.status())) {
            throw fail("getCounterEntry " + cache + "/" + key + " expected UNKNOWN_KEY (modelled absent) but got "
                    + response.status() + " value='" + response.value() + "'");
        }
    }

    /** Lazily creates a counter entry (initialised to 0) so inc/dec/set/get have a defined starting point. */
    private void ensureCounter(String cache, String key) {
        if (oracle.counterContains(cache, key)) {
            return;
        }
        command(WsOp.ADD_COUNTER_ENTRY, cache, key, null, TTL_NONE, 0, "addCounterEntry " + cache + "/" + key);
        oracle.counterPut(cache, key, 0L);
    }

    // ---------------------------------------------------------------- reconciliation

    private void reconcileAll() {
        for (var cache : kvCacheIds) {
            var actual = fetchEntries(WsOp.GET_CACHE_ENTRIES, cache);
            var expected = oracle.kvSnapshot(cache);
            if (!expected.equals(actual)) {
                throw fail("KV reconciliation mismatch for " + cache + ": " + diff(expected, actual));
            }
        }
        for (var cache : counterCacheIds) {
            var actual = fetchEntries(WsOp.GET_COUNTER_ENTRIES, cache);
            var expected = new HashMap<String, String>();
            oracle.counterSnapshot(cache).forEach((k, v) -> expected.put(k, Long.toString(v)));
            if (!expected.equals(actual)) {
                throw fail("counter reconciliation mismatch for " + cache + ": " + diff(expected, actual));
            }
        }
        report.recordVerification();
    }

    private Map<String, String> fetchEntries(WsOp op, String cache) {
        var corr = nextCorr();
        client.command(op, corr, cache, null, null, TTL_NONE, 0);
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (!client.entriesComplete.containsKey(corr)) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting " + op + " " + cache);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        var status = client.entriesStatus.remove(corr);
        client.entriesComplete.remove(corr);
        var items = client.entriesAccumulated.remove(corr);
        if (!OK.equals(status)) {
            throw fail(op + " " + cache + " returned status " + status);
        }
        return items == null ? Map.of() : items;
    }

    // ---------------------------------------------------------------- await + health

    /** Sends a command expecting SUCCESS, awaiting its response. */
    private void command(WsOp op, String cacheId, String key, String value, long ttl, long counterValue, String what) {
        var corr = nextCorr();
        client.command(op, corr, cacheId, key, value, ttl, counterValue);
        expectSuccess(awaitCommand(corr), what);
    }

    private CommandResponse awaitCommand(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        CommandResponse response;
        while ((response = client.commandResponses.remove(corr)) == null) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting response for " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        return response;
    }

    private void checkHealth() {
        if (!client.isHealthy()) {
            throw fail("bidi socket unhealthy during command phase");
        }
        if (!client.errors.isEmpty()) {
            var error = client.errors.entrySet().iterator().next();
            throw fail("bidi gateway reported an error for " + error.getKey() + ": " + error.getValue());
        }
    }

    // ---------------------------------------------------------------- assertions + helpers

    private void expectSuccess(CommandResponse response, String what) {
        if (!OK.equals(response.status())) {
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
        return "cmd-c" + (++corrSeq);
    }

    private static OpType[] buildOpTable(BidiSoakConfig cfg) {
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
            throw new IllegalArgumentException("Bidi command phase requires at least one kv or counter cache");
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
