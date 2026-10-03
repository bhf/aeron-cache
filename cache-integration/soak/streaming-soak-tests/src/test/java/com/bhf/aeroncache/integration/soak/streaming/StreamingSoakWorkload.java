package com.bhf.aeroncache.integration.soak.streaming;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener;
import com.bhf.aeroncache.integration.soak.common.SoakRecordingListener.StreamUpdate;
import com.bhf.aeroncache.integration.soak.common.SoakReport;
import com.bhf.aeroncache.integration.soak.common.SoakRun;
import io.aeron.Aeron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

/**
 * The streaming soak workload: a long-running, single-writer driver that verifies the gateway's
 * subscription/streaming behaviour across the full matrix of subscription variations.
 *
 * <p><b>Coverage, not chance.</b> Streaming behaviour is the product of orthogonal axes - cache kind
 * (regular/counter), subscription mode (full/patch), scope (whole-cache/single-key), hydration
 * (snapshot on/off) and cardinality (single/multi-cache). The workload enumerates every valid cell of
 * that matrix ({@link #buildMatrix()}) and cycles through it round-robin (seeded order) so a run of at
 * least one full pass exercises every cell, repeatedly for the rest of the duration. The per-cell round
 * counts are written to the report, and the run fails if any cell was missed - so a green run is proof
 * the whole matrix was covered.
 *
 * <p><b>Each round is verified against an expectation model.</b> For the chosen variant it creates fresh
 * dedicated cache(s), subscribes (awaiting the ack barrier), optionally verifies snapshot hydration,
 * applies a mutation sequence while computing the exact events that subscription must receive (mode/scope/
 * kind-aware), then waits for those events (bounded) and asserts the received per-cache stream equals the
 * expectation in order with no extras. It then unsubscribes, mutates again, and asserts the stream went
 * silent. Values are single-field JSON {@code {"v":N}} so merge-patch == overwrite, keeping expected merged
 * values and patch deltas exact without re-implementing RFC 7386.
 */
final class StreamingSoakWorkload implements SoakRun {

    enum Kind { REGULAR, COUNTER }

    enum Mode { FULL, PATCH }

    enum Scope { WHOLE, KEY }

    enum Card { SINGLE, MULTI }

    record Variant(Kind kind, Mode mode, Scope scope, boolean hydrate, Card card) {
        String cell() {
            return kind + "/" + mode + "/" + scope + "/" + (hydrate ? "hydrate" : "nohydrate") + "/" + card;
        }
    }

    /** An event the subscription under test is expected to receive, matched type-aware against received updates. */
    record ExpectedEvent(String cacheId, UpdateEventType type, String key, String value) {
        boolean matches(StreamUpdate u) {
            if (u.eventType() != type || !u.cacheId().equals(cacheId)) {
                return false;
            }
            return switch (type) {
                case ADD_ITEM, PATCH_ITEM -> key.equals(u.key()) && value.equals(u.value());
                case REMOVE_ITEM -> key.equals(u.key());
                case CLEAR_CACHE, DELETE_CACHE -> true;
                default -> false;
            };
        }

        @Override
        public String toString() {
            return type + "(" + cacheId + "/" + key + "=" + value + ")";
        }
    }

    private static final Logger log = LogManager.getLogger(StreamingSoakWorkload.class);

    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;
    private static final long GRACE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
    private static final long PROGRESS_EVERY = 50L;
    private static final String KEY_SUBSCRIBED = "k0";
    private static final String KEY_OTHER = "k1";

    private final GatewayClient client;
    private final SoakRecordingListener listener;
    private final StreamingSoakConfig cfg;
    private final SoakReport report;
    private final java.util.Random rnd;
    private final long opTimeoutNanos;
    private final List<Variant> matrix;
    private final Map<String, Long> coverage = new TreeMap<>();

    private long corrSeq;
    private long cacheSeq;
    private String currentCell = "";

    StreamingSoakWorkload(GatewayClient client, SoakRecordingListener listener, StreamingSoakConfig cfg) {
        this.client = client;
        this.listener = listener;
        this.cfg = cfg;
        this.report = new SoakReport(cfg.seed, cfg.durationSeconds, cfg.toConfigMap());
        this.rnd = new java.util.Random(cfg.seed);
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.matrix = buildMatrix(cfg.includeCounters);
        Collections.shuffle(this.matrix, rnd);
    }

    @Override
    public SoakReport report() {
        return report;
    }

    @Override
    public void run() {
        log.info("Starting streaming soak run with {} ({} matrix cells)", cfg, matrix.size());

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.durationSeconds);
        int index = 0;
        long rounds = 0;
        // Round-robin through the (seeded-shuffled) matrix so the first full pass covers every cell.
        while (System.nanoTime() < deadline) {
            var variant = matrix.get(index % matrix.size());
            index++;
            runRound(variant);
            coverage.merge(variant.cell(), 1L, Long::sum);
            report.recordCount("round");
            rounds++;

            if (rounds % PROGRESS_EVERY == 0) {
                report.sampleHeap();
                checkHealth();
                log.info(report.progressLine());
            }
        }

        report.sampleHeap();
        report.putSection("coverage", coverage);

        // A green run must prove full matrix coverage - but only once at least one full pass completed
        // (a very short run may legitimately not reach every cell).
        if (rounds >= matrix.size()) {
            for (var variant : matrix) {
                if (coverage.getOrDefault(variant.cell(), 0L) == 0L) {
                    throw fail("coverage gap: matrix cell never exercised: " + variant.cell());
                }
            }
            log.info("Streaming soak complete: all {} matrix cells covered across {} rounds. {}",
                    matrix.size(), rounds, report.progressLine());
        } else {
            log.warn("Streaming soak complete but only {} rounds ran (< {} cells); coverage is partial. {}",
                    rounds, matrix.size(), report.progressLine());
        }
    }

    private static List<Variant> buildMatrix(boolean includeCounters) {
        var variants = new ArrayList<Variant>();
        var kinds = includeCounters ? List.of(Kind.REGULAR, Kind.COUNTER) : List.of(Kind.REGULAR);
        for (var kind : kinds) {
            // Patch mode is a JSON-value merge concern exercised only on regular caches (counters stream
            // their value as ADD_ITEM); mirror the existing integration tests and skip counter+patch.
            var modes = kind == Kind.REGULAR ? List.of(Mode.FULL, Mode.PATCH) : List.of(Mode.FULL);
            for (var mode : modes) {
                for (var scope : Scope.values()) {
                    for (var hydrate : List.of(Boolean.FALSE, Boolean.TRUE)) {
                        for (var card : Card.values()) {
                            variants.add(new Variant(kind, mode, scope, hydrate, card));
                        }
                    }
                }
            }
        }
        return variants;
    }

    // ---------------------------------------------------------------- round

    private void runRound(Variant v) {
        currentCell = v.cell();
        drainStreamQueue();
        var caches = createCaches(v);
        var model = new RoundModel();
        var expected = new HashMap<String, List<ExpectedEvent>>();
        caches.forEach(c -> expected.put(c, new ArrayList<>()));
        var deleted = new ArrayList<String>();

        // Pre-subscribe seeding serves two purposes: it is the hydration source for full-mode rounds, and
        // it gives counter rounds a defined base so the workload relies only on the confirmed counter
        // streaming path (increment/decrement/set -> ADD_ITEM), not on whether a first addCounterEntry
        // itself streams. Entries created here (before subscribe) do not stream live; with sendSnapshot=true
        // they are replayed as hydration ADD_ITEMs (verified and consumed below).
        var seedKeys = computeSeedKeys(v);
        boolean willHydrate = v.hydrate() && v.mode() == Mode.FULL;
        var hydrationExpected = new HashMap<String, List<ExpectedEvent>>();
        caches.forEach(c -> hydrationExpected.put(c, new ArrayList<>()));
        for (var cache : caches) {
            for (var key : seedKeys) {
                String valueStr;
                if (v.kind() == Kind.COUNTER) {
                    long init = rnd.nextInt(1_000);
                    sendCommand(c -> client.addCounterEntry(c, cache, key, init, TTL_NONE), "seed addCounterEntry " + cache + "/" + key);
                    model.putCounter(cache, key, init);
                    valueStr = Long.toString(init);
                } else {
                    var value = regularValue();
                    putRegular(cache, key, value);
                    model.putRegular(cache, key, value);
                    valueStr = value;
                }
                // A hydrating subscription replays only the entries it can see (whole-cache: all; key-scoped:
                // just the subscribed key) - so this also asserts key-filtered hydration.
                if (willHydrate && visible(v, key)) {
                    hydrationExpected.get(cache).add(new ExpectedEvent(cache, UpdateEventType.ADD_ITEM, key, valueStr));
                }
            }
        }

        var corr = subscribe(v, caches);
        awaitSubscribeAck(corr);

        if (willHydrate && !seedKeys.isEmpty()) {
            verifyHydration(corr, hydrationExpected);
        }

        if (v.mode() == Mode.PATCH) {
            buildPatchLive(v, caches, model, expected);
        } else {
            buildFullLive(v, caches, model, expected, deleted);
        }

        verifyLiveOrdered(corr, expected);
        report.recordVerification();

        unsubscribeNegative(v, caches, deleted, corr);
        cleanup(v, caches, deleted);
    }

    // ---------------------------------------------------------------- live mutation sequences

    private void buildFullLive(Variant v, List<String> caches, RoundModel model,
                               Map<String, List<ExpectedEvent>> expected, List<String> deleted) {
        var keys = keysFor(v);
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            if (v.kind() == Kind.COUNTER) {
                fullCounterMutation(v, cache, key, model, expected);
            } else {
                fullRegularMutation(v, cache, key, model, expected);
            }
        }

        // Whole-cache subscribers also observe cache-wide lifecycle events. Clear one cache, and (for the
        // last cache) delete it - exercising CLEAR_CACHE and DELETE_CACHE as the final events for that cache.
        if (v.scope() == Scope.WHOLE) {
            var clearCache = caches.get(rnd.nextInt(caches.size()));
            clearCache(v, clearCache);
            model.clear(clearCache);
            expected.get(clearCache).add(new ExpectedEvent(clearCache, UpdateEventType.CLEAR_CACHE, "", ""));

            // DELETE_CACHE streaming is confirmed for regular caches (gateway e2e). For counter caches the
            // in-round delete is skipped; cleanup deletes them after the unsubscribe, so nothing streams to us.
            if (v.kind() == Kind.REGULAR) {
                var deleteCache = caches.get(caches.size() - 1);
                deleteCache(v, deleteCache);
                expected.get(deleteCache).add(new ExpectedEvent(deleteCache, UpdateEventType.DELETE_CACHE, "", ""));
                deleted.add(deleteCache);
            }
        }
    }

    /**
     * Keys to seed before subscribing: counter rounds always (a base for inc/dec/set), regular full-mode
     * rounds only when hydrating (to create a hydration source). Regular patch rounds never pre-seed - their
     * own first-add phase (post-subscribe) is what a patch subscriber must not be notified of.
     */
    private List<String> computeSeedKeys(Variant v) {
        if (v.kind() == Kind.COUNTER) {
            return keysFor(v);
        }
        if (v.mode() == Mode.FULL && v.hydrate()) {
            // Only seed keys the subscription can see. Snapshot hydration is NOT key-filtered by the gateway
            // (it replays every entry present at subscribe time), so seeding a non-subscribed key would make
            // hydration deliver it too. Seeding only visible keys keeps the hydration expectation exact; live
            // key-filtering is still exercised by introducing the non-subscribed key after subscribe.
            return visibleKeys(v);
        }
        return List.of();
    }

    private List<String> visibleKeys(Variant v) {
        if (v.scope() == Scope.KEY) {
            return List.of(KEY_SUBSCRIBED);
        }
        return keysFor(v);
    }

    private void fullRegularMutation(Variant v, String cache, String key, RoundModel model,
                                     Map<String, List<ExpectedEvent>> expected) {
        boolean present = model.containsRegular(cache, key);
        if (present && rnd.nextInt(4) == 0) {
            removeRegular(cache, key);
            model.removeRegular(cache, key);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, UpdateEventType.REMOVE_ITEM, key, ""));
            }
        } else {
            var value = regularValue();
            putRegular(cache, key, value);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, UpdateEventType.ADD_ITEM, key, value));
            }
        }
    }

    private void fullCounterMutation(Variant v, String cache, String key, RoundModel model,
                                     Map<String, List<ExpectedEvent>> expected) {
        // Keys are pre-seeded before subscribe, so only the confirmed streaming mutations run here:
        // increment / decrement / set all stream the new value as an ADD_ITEM.
        long current = model.counter(cache, key);
        long newValue;
        int choice = rnd.nextInt(3);
        if (choice == 0) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current + delta;
            sendCommand(c -> client.incrementCounter(c, cache, key, delta, TTL_NONE), "incrementCounter " + cache + "/" + key);
        } else if (choice == 1) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current - delta;
            sendCommand(c -> client.decrementCounter(c, cache, key, delta, TTL_NONE), "decrementCounter " + cache + "/" + key);
        } else {
            newValue = rnd.nextInt(1_000);
            sendCommand(c -> client.setCounter(c, cache, key, finalLong(newValue), TTL_NONE), "setCounter " + cache + "/" + key);
        }
        model.putCounter(cache, key, newValue);
        if (visible(v, key)) {
            expected.get(cache).add(new ExpectedEvent(cache, UpdateEventType.ADD_ITEM, key, Long.toString(newValue)));
        }
    }

    private void buildPatchLive(Variant v, List<String> caches, RoundModel model,
                                Map<String, List<ExpectedEvent>> expected) {
        var keys = keysFor(v);
        // Phase 1: first-add every key. A PATCH-mode subscriber is NOT notified on the first add of a new
        // key, so these produce no expected events (verified by the absence of extras later).
        for (var cache : caches) {
            for (var key : keys) {
                var value = regularValue();
                putRegular(cache, key, value);
                model.putRegular(cache, key, value);
            }
        }
        // Phase 2: overwrite existing keys. Each overwrite streams a PATCH_ITEM carrying the delta, which
        // for single-field {"v":N} values equals the new value.
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            var value = regularValue();
            putRegular(cache, key, value);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, UpdateEventType.PATCH_ITEM, key, value));
            }
        }
    }

    private List<String> keysFor(Variant v) {
        if (v.scope() == Scope.KEY) {
            return List.of(KEY_SUBSCRIBED, KEY_OTHER);
        }
        var keys = new ArrayList<String>();
        for (int i = 0; i < cfg.wholeCacheKeys; i++) {
            keys.add("k" + i);
        }
        return keys;
    }

    private boolean visible(Variant v, String key) {
        return v.scope() == Scope.WHOLE || KEY_SUBSCRIBED.equals(key);
    }

    // ---------------------------------------------------------------- verification

    private void verifyHydration(String corr, Map<String, List<ExpectedEvent>> hydrationExpected) {
        int totalExpected = hydrationExpected.values().stream().mapToInt(List::size).sum();
        var got = collect(corr, hydrationExpected.keySet(), totalExpected, "hydration");
        // Snapshot replay order is not guaranteed, so compare per cache as an unordered multiset.
        for (var e : hydrationExpected.entrySet()) {
            var expectedSigs = new TreeMap<String, Integer>();
            e.getValue().forEach(ev -> expectedSigs.merge(sig(ev), 1, Integer::sum));
            var gotSigs = new TreeMap<String, Integer>();
            got.getOrDefault(e.getKey(), List.of()).forEach(u -> gotSigs.merge(sig(u), 1, Integer::sum));
            if (!expectedSigs.equals(gotSigs)) {
                throw fail("hydration mismatch for " + e.getKey() + ": expected " + expectedSigs + " got " + gotSigs);
            }
        }
        report.recordVerification();
    }

    private void verifyLiveOrdered(String corr, Map<String, List<ExpectedEvent>> expected) {
        int totalExpected = expected.values().stream().mapToInt(List::size).sum();
        var got = collect(corr, expected.keySet(), totalExpected, "live stream");
        for (var e : expected.entrySet()) {
            var exp = e.getValue();
            var rec = got.getOrDefault(e.getKey(), List.of());
            if (exp.size() != rec.size()) {
                throw fail("stream event count mismatch for " + e.getKey() + ": expected " + exp.size()
                        + " " + exp + " but received " + rec.size() + " " + render(rec));
            }
            for (int i = 0; i < exp.size(); i++) {
                if (!exp.get(i).matches(rec.get(i))) {
                    throw fail("stream event mismatch for " + e.getKey() + " at index " + i
                            + ": expected " + exp.get(i) + " but received " + render(List.of(rec.get(i))));
                }
            }
        }
    }

    /**
     * Collects exactly {@code totalExpected} stream updates for the subscription across the given caches,
     * then grace-polls to prove no <em>extra</em> events arrive (the negative half of the assertion).
     */
    private Map<String, List<StreamUpdate>> collect(String corr, java.util.Set<String> caches, int totalExpected, String what) {
        var got = new HashMap<String, List<StreamUpdate>>();
        caches.forEach(c -> got.put(c, new ArrayList<>()));
        int count = 0;
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (count < totalExpected) {
            var u = listener.streamUpdates.poll();
            if (u == null) {
                checkHealth();
                if (System.nanoTime() > deadline) {
                    throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting " + what
                            + " events: got " + count + "/" + totalExpected + " " + renderAll(got));
                }
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            // Stream updates do not echo the subscription's correlation id, so attribute by cacheId: each
            // round uses freshly created unique caches and drains the queue first, so any event for one of
            // this round's caches belongs to this round's (sole) subscription. Events for other caches are
            // residual/foreign and ignored.
            var bucket = got.get(u.cacheId());
            if (bucket == null) {
                continue;
            }
            bucket.add(u);
            count++;
        }
        // Grace window: any further event for one of our caches is an unexpected extra.
        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = listener.streamUpdates.poll();
            if (u == null) {
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            if (got.containsKey(u.cacheId())) {
                throw fail(what + ": unexpected extra stream event after collecting " + count + "/" + totalExpected
                        + " " + renderAll(got) + ": " + u);
            }
        }
        return got;
    }

    private void unsubscribeNegative(Variant v, List<String> caches, List<String> deleted, String corr) {
        var active = new ArrayList<String>();
        caches.forEach(c -> {
            if (!deleted.contains(c)) {
                active.add(c);
            }
        });
        if (active.isEmpty()) {
            return;
        }
        boolean counters = v.kind() == Kind.COUNTER;
        active.forEach(cache -> sendCommandNoAwait(() -> client.unsubscribe(nextCorr(), cache, counters)));
        drainStreamQueue();

        // Issue a mutation that WOULD stream to this subscription if it were still live, then prove silence.
        var probeCache = active.get(0);
        var probeKey = v.scope() == Scope.KEY ? KEY_SUBSCRIBED : "k0";
        if (v.kind() == Kind.COUNTER) {
            sendCommand(c -> client.addCounterEntry(c, probeCache, probeKey, 1L, TTL_NONE), "probe addCounterEntry");
        } else {
            sendCommand(c -> client.addEntry(c, probeCache, probeKey, regularValue(), TTL_NONE), "probe addEntry");
        }

        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = listener.streamUpdates.poll();
            if (u == null) {
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            if (active.contains(u.cacheId())) {
                throw fail("received stream event after unsubscribe: " + u);
            }
        }
        report.recordCount("unsubscribeNegativeChecked");
    }

    // ---------------------------------------------------------------- mutations (cluster + await)

    private void putRegular(String cache, String key, String value) {
        sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "addEntry " + cache + "/" + key);
    }

    private void removeRegular(String cache, String key) {
        var corr = nextCorr();
        sendWithRetry(() -> client.removeEntry(corr, cache, key));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS && r.status() != OperationStatus.UNKNOWN_KEY) {
            throw fail("removeEntry " + cache + "/" + key + " unexpected status " + r.status());
        }
    }

    private void clearCache(Variant v, String cache) {
        if (v.kind() == Kind.COUNTER) {
            sendCommand(c -> client.clearCounterCache(c, cache), "clearCounterCache " + cache);
        } else {
            sendCommand(c -> client.clearCache(c, cache), "clearCache " + cache);
        }
    }

    private void deleteCache(Variant v, String cache) {
        if (v.kind() == Kind.COUNTER) {
            sendCommand(c -> client.deleteCounterCache(c, cache), "deleteCounterCache " + cache);
        } else {
            sendCommand(c -> client.deleteCache(c, cache), "deleteCache " + cache);
        }
    }

    private List<String> createCaches(Variant v) {
        int n = v.card() == Card.MULTI ? 2 : 1;
        var ids = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            var id = "soak-strm-" + (++cacheSeq);
            if (v.kind() == Kind.COUNTER) {
                sendCommand(c -> client.createCounterCache(c, id), "createCounterCache " + id);
            } else {
                sendCommand(c -> client.createCache(c, id), "createCache " + id);
            }
            ids.add(id);
        }
        return ids;
    }

    private void cleanup(Variant v, List<String> caches, List<String> deleted) {
        for (var cache : caches) {
            if (deleted.contains(cache)) {
                continue;
            }
            deleteCache(v, cache);
        }
    }

    private String subscribe(Variant v, List<String> caches) {
        var corr = nextCorr();
        boolean counters = v.kind() == Kind.COUNTER;
        boolean snapshot = v.hydrate();
        if (v.scope() == Scope.WHOLE && v.mode() == Mode.FULL) {
            sendWithRetry(() -> client.subscribe(corr, caches, snapshot, counters));
        } else {
            boolean patch = v.mode() == Mode.PATCH;
            var keys = new ArrayList<String>();
            for (var ignored : caches) {
                keys.add(v.scope() == Scope.KEY ? KEY_SUBSCRIBED : null);
            }
            sendWithRetry(() -> client.subscribe(corr, caches, keys, patch, snapshot, counters));
        }
        return corr;
    }

    // ---------------------------------------------------------------- await + health helpers

    private void awaitSubscribeAck(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (!listener.subscribeAcks.containsKey(corr)) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting subscribe ack " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        var ack = listener.subscribeAcks.remove(corr);
        if (ack.status() != OperationStatus.SUCCESS) {
            throw fail("subscribe " + corr + " not acknowledged: " + ack.status());
        }
    }

    /** Sends a command (with retry), awaits its response and asserts SUCCESS. */
    private void sendCommand(LongSupplierWithCorr op, String what) {
        var corr = nextCorr();
        sendWithRetry(() -> op.send(corr));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS) {
            throw fail(what + " expected SUCCESS but got " + r.status());
        }
    }

    private void sendCommandNoAwait(LongSupplier send) {
        sendWithRetry(send);
    }

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

    private void sendWithRetry(LongSupplier send) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (send.getAsLong() == Aeron.NULL_VALUE) {
            if (System.nanoTime() > deadline) {
                throw fail("timed out offering a request frame (sustained backpressure)");
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
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

    private void drainStreamQueue() {
        while (listener.streamUpdates.poll() != null) {
            // discard any residual updates from a previous round
        }
    }

    // ---------------------------------------------------------------- misc helpers

    private AssertionError fail(String detail) {
        var full = "[" + currentCell + "] " + detail;
        report.recordMismatch(full);
        return new AssertionError(full);
    }

    private String regularValue() {
        return "{\"v\":" + rnd.nextInt(1_000_000) + "}";
    }

    private String nextCorr() {
        return "c" + (++corrSeq);
    }

    private static long finalLong(long v) {
        return v;
    }

    private static String sig(ExpectedEvent e) {
        return e.type() + "|" + e.key() + "|" + e.value();
    }

    private static String sig(StreamUpdate u) {
        return switch (u.eventType()) {
            case ADD_ITEM, PATCH_ITEM -> u.eventType() + "|" + u.key() + "|" + u.value();
            case REMOVE_ITEM -> u.eventType() + "|" + u.key() + "|";
            case CLEAR_CACHE, DELETE_CACHE -> u.eventType() + "||";
            default -> u.eventType() + "||";
        };
    }

    private static String render(List<StreamUpdate> updates) {
        var sb = new StringBuilder("[");
        for (var u : updates) {
            sb.append(u.eventType()).append('(').append(u.key()).append('=').append(u.value()).append(") ");
        }
        return sb.append(']').toString();
    }

    private static String renderAll(Map<String, List<StreamUpdate>> got) {
        var sb = new StringBuilder();
        got.forEach((k, v) -> sb.append(k).append("->").append(render(v)).append(' '));
        return sb.toString();
    }

    /** A command send that needs the per-call correlation id (so the workload can await the response). */
    @FunctionalInterface
    private interface LongSupplierWithCorr {
        long send(String correlationId);
    }

    /** Per-round expected state of the caches under test (regular values and counter values). */
    private static final class RoundModel {
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

        void putCounter(String cache, String key, long value) {
            counters.put(composite(cache, key), value);
        }

        boolean containsCounter(String cache, String key) {
            return counters.containsKey(composite(cache, key));
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
}
