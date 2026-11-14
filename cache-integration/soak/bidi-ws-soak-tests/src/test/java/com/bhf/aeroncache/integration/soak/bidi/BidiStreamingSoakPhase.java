package com.bhf.aeroncache.integration.soak.bidi;

import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.CommandResponse;
import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.Selector;
import com.bhf.aeroncache.integration.soak.bidi.BidiSoakClient.StreamUpdate;
import com.bhf.aeroncache.integration.soak.common.NearCacheAdmin;
import com.bhf.aeroncache.integration.soak.common.SoakPayloads;
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

    record Variant(Kind kind, Mode mode, Scope scope, boolean hydrate, Card card, boolean warm) {
        String cell() {
            return kind + "/" + mode + "/" + scope + "/" + (hydrate ? "hydrate" : "nohydrate") + "/" + card
                    + (warm ? "/warm" : "");
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

    /** A value that is not parseable JSON, so no merge patch can be computed and the whole value is sent. */
    private static final String UNPARSEABLE_VALUE = "not json";

    private static final String OUTCOME_DELTA = "delta";
    private static final String OUTCOME_SUPPRESSED = "suppressed";
    private static final String OUTCOME_FALLBACK = "full_fallback";

    private final BidiSoakClient mutator;
    private final BidiSoakClient subscriber;
    private final NearCacheAdmin nearCacheAdmin;
    private final BidiSoakConfig cfg;
    private final SoakReport report;
    private final Random rnd;
    private final long opTimeoutNanos;
    private final List<Variant> matrix;
    private final Map<String, Long> coverage = new TreeMap<>();
    private final Map<String, Long> nearPatchOutcomes = new TreeMap<>();
    private final SoakPayloads payloads;

    // Size-aware keys, derived once from the payload generator. The subscribed/other keys live inside the
    // k0..kN whole-cache range; the delta/suppress/fallback keys are kept outside it so the random overwrite
    // phase never touches them.
    private final String keySubscribed;
    private final String keyOther;
    private final String keyDelta;
    private final String keySuppress;
    private final String keyFallback;

    private long corrSeq;
    private long cacheSeq;
    private String currentCell = "";

    BidiStreamingSoakPhase(BidiSoakClient mutator, BidiSoakClient subscriber, NearCacheAdmin nearCacheAdmin,
                           BidiSoakConfig cfg, SoakReport report, Random rnd) {
        this.mutator = mutator;
        this.subscriber = subscriber;
        this.nearCacheAdmin = nearCacheAdmin;
        this.cfg = cfg;
        this.report = report;
        this.rnd = rnd;
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.matrix = buildMatrix(cfg.includeCounters, cfg.includeSynthetic);
        Collections.shuffle(this.matrix, rnd);
        // Streaming values are single-field JSON, so a 0 value size keeps the natural {"v":N} form.
        this.payloads = new SoakPayloads(cfg.keySizeBytes, cfg.valueSizeBytes);
        this.keySubscribed = payloads.key("0");
        this.keyOther = payloads.key("1");
        this.keyDelta = payloads.key("d");
        this.keySuppress = payloads.key("s");
        this.keyFallback = payloads.key("f");
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
        if (!nearPatchOutcomes.isEmpty()) {
            report.putSection("nearPatch", nearPatchOutcomes);
        }

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

    private static List<Variant> buildMatrix(boolean includeCounters, boolean includeSynthetic) {
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
                            variants.add(new Variant(kind, mode, scope, hydrate, card, false));
                        }
                    }
                }
            }
        }
        // Synthetic patch generation (near cache): a hydrating PATCH subscription to a WARM (primed) regular
        // cache is served merge-patch deltas computed locally from the warm copy. Regular-only and always
        // hydrating, so these cells layer a `warm` flag onto the regular/patch/hydrate variants across both
        // scopes and cardinalities.
        if (includeSynthetic) {
            for (var scope : Scope.values()) {
                for (var card : Card.values()) {
                    variants.add(new Variant(Kind.REGULAR, Mode.PATCH, scope, true, card, true));
                }
            }
        }
        return variants;
    }

    // ---------------------------------------------------------------- round

    private void runRound(Variant v) {
        currentCell = v.cell();
        drainStreamQueue();
        if (v.warm()) {
            runWarmRound(v);
            return;
        }
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
            return List.of(keySubscribed);
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
        // Phase 1: first-add every key. A PATCH-mode subscriber present for the first insert of a key is
        // notified with the whole value as a full ADD_ITEM baseline (there is no prior value to diff against),
        // so a subscriber that joined before the key existed is not left holding a stale (empty) view.
        for (var cache : caches) {
            for (var key : keys) {
                addFirst(v, cache, key, model, expected);
            }
        }
        // Phase 2: overwrite existing keys. Each overwrite streams a PATCH_ITEM carrying the delta, which for
        // single-field {"v":N} values equals the new value.
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            overwriteDelta(v, cache, key, model, expected);
        }
        // Phase 3: deterministic coverage of the two remaining patch outcomes on dedicated keys (kept outside
        // the random key range so Phase 2 never collides with them): a no-op overwrite that is SUPPRESSED (no
        // event at all), and an overwrite whose value is not parseable JSON, which falls back to a full
        // ADD_ITEM rather than leaving the subscriber stale.
        appendPatchOutcomeTail(v, caches, model, expected);
    }

    /**
     * Append the suppressed and full-fallback patch outcomes. In whole-cache scope they run on their own
     * dedicated keys (each first-added, which itself emits a baseline ADD_ITEM); in single-key scope only the
     * subscribed key is visible, so they run in sequence on it (it already exists from Phase 1).
     */
    private void appendPatchOutcomeTail(Variant v, List<String> caches, RoundModel model,
                                        Map<String, List<ExpectedEvent>> expected) {
        for (var cache : caches) {
            if (v.scope() == Scope.WHOLE) {
                addFirst(v, cache, keySuppress, model, expected);
                overwriteSuppressed(cache, keySuppress, model);
                addFirst(v, cache, keyFallback, model, expected);
                overwriteFallback(v, cache, keyFallback, model, expected);
            } else {
                overwriteSuppressed(cache, keySubscribed, model);
                overwriteFallback(v, cache, keySubscribed, model, expected);
            }
        }
    }

    // ---------------------------------------------------------------- warm (synthetic patch) round

    /**
     * A round that exercises near-cache synthetic patch generation over the bidi protocol: seed a regular
     * cache, prime it so the warm local copy populates, then open a hydrating PATCH subscription served from
     * the warm copy. The subscription receives a full ADD_ITEM baseline per warm key on hydration, then one synthetic
     * event per live mutation - a PATCH_ITEM delta, a full ADD_ITEM fallback (unparseable value), and nothing for
     * a no-op (suppressed) overwrite. The per-outcome
     * {@code aeroncache_near_patches_total} counter is asserted to have advanced by exactly the expected amount,
     * proving the deltas were synthesised from the warm copy rather than taken on the cluster path.
     */
    private void runWarmRound(Variant v) {
        var caches = createCaches(v);
        var model = new RoundModel();
        var seedKeys = warmSeedKeys(v);

        for (var cache : caches) {
            for (var key : seedKeys) {
                var value = regularValue();
                mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "warm seed " + cache + "/" + key);
                model.putRegular(cache, key, value);
            }
            nearCacheAdmin.prime(cache);
            nearCacheAdmin.awaitWarm(cache, seedKeys.size(), opTimeoutNanos);
        }

        var expected = new HashMap<String, List<ExpectedEvent>>();
        var hydrationExpected = new HashMap<String, List<ExpectedEvent>>();
        caches.forEach(c -> {
            expected.put(c, new ArrayList<>());
            hydrationExpected.put(c, new ArrayList<>());
        });
        for (var cache : caches) {
            for (var key : seedKeys) {
                if (visible(v, key)) {
                    hydrationExpected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, model.regular(cache, key)));
                }
            }
        }

        var corr = subscribe(v, caches);
        awaitSubscribeAck(corr);
        verifyHydration(hydrationExpected);

        boolean assertMetrics = nearCacheAdmin.supportsPatchMetrics();
        var before = assertMetrics ? snapshotOutcomes() : null;
        var increments = new TreeMap<String, Long>();
        for (var cache : caches) {
            if (v.scope() == Scope.WHOLE) {
                overwriteDelta(v, cache, keyDelta, model, expected);
                overwriteSuppressed(cache, keySuppress, model);
                overwriteFallback(v, cache, keyFallback, model, expected);
            } else {
                overwriteDelta(v, cache, keySubscribed, model, expected);
                overwriteSuppressed(cache, keySubscribed, model);
                overwriteFallback(v, cache, keySubscribed, model, expected);
            }
            increments.merge(OUTCOME_DELTA, 1L, Long::sum);
            increments.merge(OUTCOME_SUPPRESSED, 1L, Long::sum);
            increments.merge(OUTCOME_FALLBACK, 1L, Long::sum);
        }

        verifyLiveOrdered(expected);
        report.recordVerification();

        if (assertMetrics) {
            increments.forEach((outcome, delta) -> {
                awaitPatchCount(outcome, before.get(outcome) + delta);
                nearPatchOutcomes.merge(outcome, delta, Long::sum);
            });
            report.recordVerification();
        }

        boolean counters = false;
        caches.forEach(cache -> subscriber.unsubscribe(nextCorr(), cache, counters));
        drainStreamQueue();
        for (var cache : caches) {
            nearCacheAdmin.stopNear(cache);
        }
        cleanup(v, caches, List.of());
    }

    /** The keys seeded (and warmed) before a synthetic-patch round: one per outcome in whole scope; just the
     * subscribed key in single-key scope (the only visible key). */
    private List<String> warmSeedKeys(Variant v) {
        if (v.scope() == Scope.KEY) {
            return List.of(keySubscribed);
        }
        return List.of(keyDelta, keySuppress, keyFallback);
    }

    private Map<String, Long> snapshotOutcomes() {
        var snapshot = new TreeMap<String, Long>();
        for (var outcome : List.of(OUTCOME_DELTA, OUTCOME_SUPPRESSED, OUTCOME_FALLBACK)) {
            snapshot.put(outcome, nearCacheAdmin.patchCount(outcome));
        }
        return snapshot;
    }

    private void awaitPatchCount(String outcome, long target) {
        var deadline = System.nanoTime() + opTimeoutNanos;
        long seen;
        while ((seen = nearCacheAdmin.patchCount(outcome)) < target) {
            if (System.nanoTime() > deadline) {
                throw fail("near patch counter '" + outcome + "' did not reach " + target + " (last " + seen + ")");
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        if (seen != target) {
            throw fail("near patch counter '" + outcome + "' overshot: expected " + target + " but saw " + seen);
        }
    }

    // ---------------------------------------------------------------- single-key patch outcome primitives

    /** First-add a fresh key with a valid value; a visible first-add streams a full ADD_ITEM baseline. */
    private void addFirst(Variant v, String cache, String key, RoundModel model,
                          Map<String, List<ExpectedEvent>> expected) {
        var value = regularValue();
        mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "patch first-add " + cache + "/" + key);
        model.putRegular(cache, key, value);
        if (visible(v, key)) {
            expected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, value));
        }
    }

    /** Overwrite an existing key with a changed value; a visible overwrite streams a PATCH_ITEM delta (== the
     * new value for single-field {@code {"v":N}} values). */
    private void overwriteDelta(Variant v, String cache, String key, RoundModel model,
                                Map<String, List<ExpectedEvent>> expected) {
        var value = regularValueDiffering(model.regular(cache, key));
        mutate(WsOp.ADD_CACHE_ENTRY, cache, key, value, 0, "patch overwrite " + cache + "/" + key);
        model.putRegular(cache, key, value);
        if (visible(v, key)) {
            expected.get(cache).add(new ExpectedEvent(cache, PATCH_ITEM, key, value));
        }
    }

    /** Re-add an existing key with its current value; the merge patch is empty so the update is SUPPRESSED (no
     * event is streamed). The model value is unchanged. */
    private void overwriteSuppressed(String cache, String key, RoundModel model) {
        mutate(WsOp.ADD_CACHE_ENTRY, cache, key, model.regular(cache, key), 0, "patch no-op " + cache + "/" + key);
    }

    /** Overwrite an existing key with a value that is not parseable JSON; no merge patch can be built, so the
     * whole value is streamed as a full ADD_ITEM fallback rather than nothing. */
    private void overwriteFallback(Variant v, String cache, String key, RoundModel model,
                                   Map<String, List<ExpectedEvent>> expected) {
        mutate(WsOp.ADD_CACHE_ENTRY, cache, key, UNPARSEABLE_VALUE, 0, "patch fallback " + cache + "/" + key);
        model.putRegular(cache, key, UNPARSEABLE_VALUE);
        if (visible(v, key)) {
            expected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, UNPARSEABLE_VALUE));
        }
    }

    private List<String> keysFor(Variant v) {
        if (v.scope() == Scope.KEY) {
            return List.of(keySubscribed, keyOther);
        }
        var keys = new ArrayList<String>();
        for (int i = 0; i < cfg.wholeCacheKeys; i++) {
            keys.add(payloads.key(Integer.toString(i)));
        }
        return keys;
    }

    private boolean visible(Variant v, String key) {
        return v.scope() == Scope.WHOLE || keySubscribed.equals(key);
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
        // Whole-cache scope: k0 is the first whole-cache key, which is the subscribed key.
        var probeKey = keySubscribed;
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
            var key = v.scope() == Scope.KEY ? keySubscribed : null;
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
        return payloads.jsonValue(rnd);
    }

    /** A single-field {@code {"v":...}} value guaranteed to differ from {@code current}, so an overwrite always
     * changes the value (an accidental repeat would be suppressed with no event and break the 1:1 expectation). */
    private String regularValueDiffering(String current) {
        String value;
        do {
            value = regularValue();
        } while (value.equals(current));
        return value;
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
}
