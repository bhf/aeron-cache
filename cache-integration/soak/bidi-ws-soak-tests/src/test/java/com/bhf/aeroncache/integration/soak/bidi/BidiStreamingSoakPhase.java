package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.CommandResponse;
import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.Selector;
import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.StreamUpdate;
import com.bhf.aeroncache.integration.soak.common.SoakReport;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * The streaming phase of the bidi soak: a coverage-driven, time-bounded driver that verifies the gateway's
 * subscription/streaming behaviour <em>over the bidi protocol</em> across the full matrix of subscription
 * variations - cache kind (regular/counter), mode (full/patch), scope (whole-cache/single-key), snapshot
 * hydration and cardinality (single/multi-cache). It is the bidi counterpart of the Aeron streaming soak and
 * reuses its coverage-then-verify structure and expectation model.
 *
 * <p><b>Cross-session topology.</b> This phase mutates over one bidi socket ({@code mutator}) and
 * subscribes/observes over a separate one ({@code subscriber}) - mirroring the validated bidi streaming
 * integration tests (mutations over a different session than the subscription). Regular-cache mutations stream
 * one event per visible change; counter mutations stream <em>two</em> identical consecutive ADD_ITEMs (the
 * cluster reuses the counter result as the subscriber broadcast), a quirk that holds over bidi even across
 * sessions - the workload models it exactly, as the Aeron streaming soak does.
 *
 * <p>Each round creates fresh unique caches, subscribes (awaiting the ack barrier), optionally verifies
 * snapshot hydration, applies a mutation sequence while computing the exact events the subscription must
 * receive, asserts the received stream equals that expectation in order with no extras, then unsubscribes,
 * mutates again and asserts silence. Per-cell round counts are written to the report; the run fails if any
 * matrix cell was missed once a full pass has completed - so a green run proves the whole matrix was covered.
 */
final class BidiStreamingSoakPhase {

    enum Kind { REGULAR, COUNTER }

    enum Mode { FULL, PATCH }

    enum Scope { WHOLE, KEY }

    enum Card { SINGLE, MULTI }

    record Variant(Kind kind, Mode mode, Scope scope, boolean hydrate, Card card) {
        String cell() {
            return kind + "/" + mode + "/" + scope + "/" + (hydrate ? "hydrate" : "nohydrate") + "/" + card;
        }
    }

    /** An event the subscription under test is expected to receive, matched against received updates. */
    record ExpectedEvent(String cacheId, String type, String key, String value) {
        boolean matches(StreamUpdate u) {
            if (!type.equals(u.eventType()) || !cacheId.equals(u.cacheId())) {
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

    private static final Logger log = LogManager.getLogger(BidiStreamingSoakPhase.class);

    private static final String ADD_ITEM = "ADD_ITEM";
    private static final String PATCH_ITEM = "PATCH_ITEM";
    private static final String REMOVE_ITEM = "REMOVE_ITEM";
    private static final String CLEAR_CACHE = "CLEAR_CACHE";
    private static final String DELETE_CACHE = "DELETE_CACHE";

    private static final String OK = "SUCCESS";
    private static final String UNKNOWN_KEY = "UNKNOWN_KEY";
    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;
    private static final long GRACE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
    private static final long PROGRESS_EVERY = 50L;
    private static final String KEY_SUBSCRIBED = "k0";
    private static final String KEY_OTHER = "k1";

    private final BidiSoakClient mutator;
    private final BidiSoakClient subscriber;
    private final BidiSoakConfig cfg;
    private final SoakReport report;
    private final Random rnd;
    private final long opTimeoutNanos;
    private final List<Variant> matrix;
    private final Map<String, Long> coverage = new TreeMap<>();

    private long corrSeq;
    private long cacheSeq;
    private String currentCell = "";

    BidiStreamingSoakPhase(BidiSoakClient mutator, BidiSoakClient subscriber, BidiSoakConfig cfg,
                           SoakReport report, Random rnd) {
        this.mutator = mutator;
        this.subscriber = subscriber;
        this.cfg = cfg;
        this.report = report;
        this.rnd = rnd;
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.matrix = buildMatrix(cfg.includeCounters);
        Collections.shuffle(this.matrix, rnd);
    }

    void run(long deadlineNanos) {
        log.info("Starting bidi streaming phase ({} matrix cells)", matrix.size());

        int index = 0;
        long rounds = 0;
        while (System.nanoTime() < deadlineNanos) {
            var variant = matrix.get(index % matrix.size());
            index++;
            runRound(variant);
            coverage.merge(variant.cell(), 1L, Long::sum);
            report.recordCount("round");
            rounds++;

            if (rounds % PROGRESS_EVERY == 0) {
                report.sampleHeap();
                checkHealth();
                log.info("streaming phase: {}", report.progressLine());
            }
        }

        report.sampleHeap();
        report.putSection("coverage", coverage);

        if (rounds >= matrix.size()) {
            for (var variant : matrix) {
                if (coverage.getOrDefault(variant.cell(), 0L) == 0L) {
                    throw fail("coverage gap: matrix cell never exercised: " + variant.cell());
                }
            }
            log.info("Bidi streaming phase complete: all {} matrix cells covered across {} rounds. {}",
                    matrix.size(), rounds, report.progressLine());
        } else {
            log.warn("Bidi streaming phase complete but only {} rounds ran (< {} cells); coverage is partial. {}",
                    rounds, matrix.size(), report.progressLine());
        }
    }

    private static List<Variant> buildMatrix(boolean includeCounters) {
        var variants = new ArrayList<Variant>();
        var kinds = includeCounters ? List.of(Kind.REGULAR, Kind.COUNTER) : List.of(Kind.REGULAR);
        for (var kind : kinds) {
            // Patch mode is a JSON-value merge concern exercised only on regular caches (counters stream
            // their value as ADD_ITEM); mirror the integration tests and skip counter+patch.
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

        // Pre-subscribe seeding is the hydration source for full-mode rounds and gives counter rounds a
        // defined base for inc/dec/set. Entries created here (before the subscriber subscribes) do not stream
        // live to the subscriber; with sendSnapshot they are replayed as hydration ADD_ITEMs.
        var seedKeys = computeSeedKeys(v);
        boolean willHydrate = v.hydrate() && v.mode() == Mode.FULL;
        var hydrationExpected = new HashMap<String, List<ExpectedEvent>>();
        caches.forEach(c -> hydrationExpected.put(c, new ArrayList<>()));
        for (var cache : caches) {
            for (var key : seedKeys) {
                String valueStr;
                if (v.kind() == Kind.COUNTER) {
                    long init = rnd.nextInt(1_000);
                    mutate(WsOp.ADD_COUNTER_ENTRY, cache, key, null, init, "seed addCounterEntry " + cache + "/" + key);
                    model.putCounter(cache, key, init);
                    valueStr = Long.toString(init);
                } else {
                    var value = regularValue();
                    mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "seed addEntry " + cache + "/" + key);
                    model.putRegular(cache, key, value);
                    valueStr = value;
                }
                // Snapshot hydration is NOT key-filtered by the gateway (accepted quirk): it replays every
                // entry present at subscribe time regardless of the subscription's key filter.
                if (willHydrate) {
                    hydrationExpected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, valueStr));
                }
            }
        }

        var corr = subscribe(v, caches);
        awaitSubscribeAck(corr);

        if (willHydrate && !seedKeys.isEmpty()) {
            verifyHydration(hydrationExpected);
        }

        if (v.mode() == Mode.PATCH) {
            buildPatchLive(v, caches, model, expected);
        } else {
            buildFullLive(v, caches, model, expected, deleted);
        }

        verifyLiveOrdered(expected);
        report.recordVerification();

        unsubscribeNegative(v, caches, deleted);
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

        // Whole-cache subscribers also observe cache-wide lifecycle events. Exercise CLEAR_CACHE and
        // DELETE_CACHE on regular caches only - mirroring the Aeron streaming soak (counter cache-wide
        // streaming to a self-subscribed session isn't pinned down; counter correctness is covered by the
        // command phase). Counter caches are torn down silently in cleanup after the unsubscribe.
        if (v.scope() == Scope.WHOLE && v.kind() == Kind.REGULAR) {
            var clearCache = caches.get(rnd.nextInt(caches.size()));
            mutate(WsOp.CLEAR_CACHE, clearCache, null, null, 0, "clearCache " + clearCache);
            model.clear(clearCache);
            expected.get(clearCache).add(new ExpectedEvent(clearCache, CLEAR_CACHE, "", ""));

            var deleteCache = caches.get(caches.size() - 1);
            mutate(WsOp.DELETE_CACHE, deleteCache, null, null, 0, "deleteCache " + deleteCache);
            expected.get(deleteCache).add(new ExpectedEvent(deleteCache, DELETE_CACHE, "", ""));
            deleted.add(deleteCache);
        }
    }

    private List<String> computeSeedKeys(Variant v) {
        if (v.kind() == Kind.COUNTER) {
            return keysFor(v);
        }
        if (v.mode() == Mode.FULL && v.hydrate()) {
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
                expected.get(cache).add(new ExpectedEvent(cache, REMOVE_ITEM, key, ""));
            }
        } else {
            var value = regularValue();
            mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "addEntry " + cache + "/" + key);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, value));
            }
        }
    }

    private void fullCounterMutation(Variant v, String cache, String key, RoundModel model,
                                     Map<String, List<ExpectedEvent>> expected) {
        // Keys are pre-seeded, so only the confirmed streaming mutations run here: increment / decrement / set
        // all stream the new value as a single ADD_ITEM to the (separate) subscriber session.
        long current = model.counter(cache, key);
        long newValue;
        int choice = rnd.nextInt(3);
        if (choice == 0) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current + delta;
            mutate(WsOp.INCREMENT_COUNTER_ENTRY, cache, key, null, delta, "incrementCounter " + cache + "/" + key);
        } else if (choice == 1) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current - delta;
            mutate(WsOp.DECREMENT_COUNTER_ENTRY, cache, key, null, delta, "decrementCounter " + cache + "/" + key);
        } else {
            newValue = rnd.nextInt(1_000);
            mutate(WsOp.SET_COUNTER_ENTRY, cache, key, null, newValue, "setCounter " + cache + "/" + key);
        }
        model.putCounter(cache, key, newValue);
        if (visible(v, key)) {
            // Accepted quirk (confirmed over bidi, as over Aeron): each counter update streams to the
            // subscriber TWICE - the cluster reuses the single increment/decrement/set result as the
            // subscriber broadcast, so two identical ADD_ITEMs arrive consecutively per cache. This holds even
            // though the mutator and subscriber are separate sessions here, so model exactly two.
            var event = new ExpectedEvent(cache, ADD_ITEM, key, Long.toString(newValue));
            expected.get(cache).add(event);
            expected.get(cache).add(event);
        }
    }

    private void buildPatchLive(Variant v, List<String> caches, RoundModel model,
                                Map<String, List<ExpectedEvent>> expected) {
        var keys = keysFor(v);
        // Phase 1: first-add every key. A PATCH-mode subscriber is NOT notified on the first add of a new key,
        // so these produce no expected events (verified by the absence of extras later).
        for (var cache : caches) {
            for (var key : keys) {
                var value = regularValue();
                mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "patch first-add " + cache + "/" + key);
                model.putRegular(cache, key, value);
            }
        }
        // Phase 2: overwrite existing keys. Each overwrite streams a PATCH_ITEM carrying the delta, which for
        // single-field {"v":N} values equals the new value.
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            var value = regularValue();
            mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "patch overwrite " + cache + "/" + key);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, PATCH_ITEM, key, value));
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

    private void verifyHydration(Map<String, List<ExpectedEvent>> hydrationExpected) {
        int totalExpected = hydrationExpected.values().stream().mapToInt(List::size).sum();
        var got = collect(hydrationExpected.keySet(), totalExpected, "hydration");
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

    private void verifyLiveOrdered(Map<String, List<ExpectedEvent>> expected) {
        int totalExpected = expected.values().stream().mapToInt(List::size).sum();
        var got = collect(expected.keySet(), totalExpected, "live stream");
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
     * Collects exactly {@code totalExpected} stream updates for the subscription across the given caches, then
     * grace-polls to prove no <em>extra</em> events arrive (the negative half of the assertion).
     */
    private Map<String, List<StreamUpdate>> collect(Set<String> caches, int totalExpected, String what) {
        var got = new HashMap<String, List<StreamUpdate>>();
        caches.forEach(c -> got.put(c, new ArrayList<>()));
        int count = 0;
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (count < totalExpected) {
            var u = subscriber.streamUpdates.poll();
            if (u == null) {
                checkHealth();
                if (System.nanoTime() > deadline) {
                    throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting " + what
                            + " events: got " + count + "/" + totalExpected + " " + renderAll(got));
                }
                LockSupport.parkNanos(PARK_NANOS);
                continue;
            }
            // Each round uses freshly created unique caches and drains the queue first, so any event for one
            // of this round's caches belongs to this round's subscription. Foreign/residual events ignored.
            var bucket = got.get(u.cacheId());
            if (bucket == null) {
                continue;
            }
            bucket.add(u);
            count++;
        }
        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = subscriber.streamUpdates.poll();
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

    private void unsubscribeNegative(Variant v, List<String> caches, List<String> deleted) {
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
        active.forEach(cache -> subscriber.unsubscribe(nextCorr(), cache, counters));
        drainStreamQueue();

        // Issue a mutation that WOULD stream to this subscription if it were still live, then prove silence.
        var probeCache = active.get(0);
        var probeKey = v.scope() == Scope.KEY ? KEY_SUBSCRIBED : "k0";
        if (v.kind() == Kind.COUNTER) {
            mutate(WsOp.INCREMENT_COUNTER_ENTRY, probeCache, probeKey, null, 1L, "probe incrementCounter");
        } else {
            mutate(WsOp.ADD_CACHE_ENTRY, probeCache, probeKey, regularValue(), 0, "probe addEntry");
        }

        var graceDeadline = System.nanoTime() + GRACE_NANOS;
        while (System.nanoTime() < graceDeadline) {
            var u = subscriber.streamUpdates.poll();
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

    // ---------------------------------------------------------------- mutations (over the mutator socket)

    private void removeRegular(String cache, String key) {
        var corr = nextCorr();
        mutator.command(WsOp.REMOVE_CACHE_ENTRY, corr, cache, key, null, TTL_NONE, 0);
        var r = awaitMutator(corr);
        if (!OK.equals(r.status()) && !UNKNOWN_KEY.equals(r.status())) {
            throw fail("removeEntry " + cache + "/" + key + " unexpected status " + r.status());
        }
    }

    private List<String> createCaches(Variant v) {
        int n = v.card() == Card.MULTI ? 2 : 1;
        var ids = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            var id = "soak-bidi-strm-" + (++cacheSeq);
            if (v.kind() == Kind.COUNTER) {
                mutate(WsOp.CREATE_COUNTER_CACHE, id, null, null, 0, "createCounterCache " + id);
            } else {
                mutate(WsOp.CREATE_CACHE, id, null, null, 0, "createCache " + id);
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
            if (v.kind() == Kind.COUNTER) {
                mutate(WsOp.DELETE_COUNTER_CACHE, cache, null, null, 0, "deleteCounterCache " + cache);
            } else {
                mutate(WsOp.DELETE_CACHE, cache, null, null, 0, "deleteCache " + cache);
            }
        }
    }

    private String subscribe(Variant v, List<String> caches) {
        var corr = nextCorr();
        boolean counters = v.kind() == Kind.COUNTER;
        boolean snapshot = v.hydrate();
        var mode = v.mode() == Mode.PATCH ? "PATCH" : "FULL";
        var selectors = new ArrayList<Selector>();
        for (var cache : caches) {
            var key = v.scope() == Scope.KEY ? KEY_SUBSCRIBED : null;
            selectors.add(new Selector(cache, key, mode));
        }
        subscriber.subscribe(corr, selectors, counters, snapshot);
        return corr;
    }

    // ---------------------------------------------------------------- await + health helpers

    private void awaitSubscribeAck(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        while (!subscriber.subscribeAcks.containsKey(corr)) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting subscribe ack " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        subscriber.subscribeAcks.remove(corr);
    }

    /** Sends a mutation over the mutator socket, awaits its response and asserts SUCCESS. */
    private void mutate(WsOp op, String cacheId, String key, String value, long counterValue, String what) {
        var corr = nextCorr();
        mutator.command(op, corr, cacheId, key, value, TTL_NONE, counterValue);
        var r = awaitMutator(corr);
        if (!OK.equals(r.status())) {
            throw fail(what + " expected SUCCESS but got " + r.status());
        }
    }

    private CommandResponse awaitMutator(String corr) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        CommandResponse response;
        while ((response = mutator.commandResponses.remove(corr)) == null) {
            checkHealth();
            if (System.nanoTime() > deadline) {
                throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting mutator response for " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        return response;
    }

    private void checkHealth() {
        if (!mutator.isHealthy() || !subscriber.isHealthy()) {
            throw fail("bidi socket unhealthy during streaming phase");
        }
        if (!mutator.errors.isEmpty()) {
            var error = mutator.errors.entrySet().iterator().next();
            throw fail("bidi mutator reported an error for " + error.getKey() + ": " + error.getValue());
        }
        if (!subscriber.errors.isEmpty()) {
            var error = subscriber.errors.entrySet().iterator().next();
            throw fail("bidi subscriber reported an error for " + error.getKey() + ": " + error.getValue());
        }
    }

    private void drainStreamQueue() {
        while (subscriber.streamUpdates.poll() != null) {
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
        return "strm-c" + (++corrSeq);
    }

    private static String sig(ExpectedEvent e) {
        return e.type() + "|" + e.key() + "|" + e.value();
    }

    private static String sig(StreamUpdate u) {
        return switch (u.eventType()) {
            case ADD_ITEM, PATCH_ITEM -> u.eventType() + "|" + u.key() + "|" + u.value();
            case REMOVE_ITEM -> u.eventType() + "|" + u.key() + "|";
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
