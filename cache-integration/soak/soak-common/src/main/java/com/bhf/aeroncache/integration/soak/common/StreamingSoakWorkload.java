package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static com.bhf.aeroncache.integration.soak.common.Variant.Mode;
import static com.bhf.aeroncache.integration.soak.common.Variant.Scope;

/**
 * The streaming soak workload: a long-running, single-writer driver that verifies the gateway's
 * subscription/streaming behaviour across the full matrix of subscription variations - cache kind
 * (regular/counter), mode (full/patch), scope (whole-cache/single-key), snapshot hydration, cardinality
 * (single/multi-cache) and warm near-cache synthetic-patch generation.
 *
 * <p><b>Split roles.</b> Mutations are applied over the binary Aeron {@link GatewayClient} (via the shared
 * {@link GatewayCommandDriver}), while the subscription under test is observed through an injected
 * {@link SoakStreamSubscriberFactory} - the one seam that differs between transports. The binary Aeron, streaming
 * WebSocket and SSE soaks all drive this same engine; they differ only in the factory they wire and in two
 * transport-determined flags (whether a counter update streams twice, and the cache-id prefix). Each round opens
 * a fresh subscriber for the variant's caches, verifies snapshot hydration if requested, applies a mutation
 * sequence while computing the exact events the subscription must receive (via {@link StreamVerifier}), asserts
 * the received stream equals that expectation in order with no extras, then closes the subscriber.
 *
 * <p>The matrix is cycled round-robin in a seeded-shuffled order so the first full pass exercises every cell, and
 * the run fails if any cell was missed once a full pass completed, so a green run proves full coverage. Values
 * are single-field JSON ({@code {"v":N}}, or {@code {"v":"<sized string>"}} when a value size is configured) so
 * merge-patch == overwrite; keys and value size come from the shared {@link SoakPayloads} generator.
 *
 * <p><b>Patch semantics.</b> A PATCH-mode subscriber present for a key's first insert receives the whole value as
 * a full {@code ADD_ITEM} baseline; a changed overwrite streams a {@code PATCH_ITEM} delta; a no-op overwrite is
 * suppressed (no event); and an overwrite whose value is not parseable JSON falls back to a full {@code ADD_ITEM}
 * rather than leaving the subscriber stale. The {@code warm} dimension additionally exercises near-cache
 * synthetic patch generation: the regular cache is primed before a hydrating patch subscription, so those deltas
 * are synthesised locally from the warm copy - asserted via the edge's {@code near_patches_total{outcome}}
 * counter through the injected {@link NearCacheAdmin}.
 */
public final class StreamingSoakWorkload implements SoakRun, SoakFailures {

    private static final Logger log = LogManager.getLogger(StreamingSoakWorkload.class);

    private static final long PARK_NANOS = 50_000L;
    private static final long PROGRESS_EVERY = 50L;

    /** A value that is not parseable JSON, so no merge patch can be computed and the whole value is sent. */
    private static final String UNPARSEABLE_VALUE = "not json";

    private static final String OUTCOME_DELTA = "delta";
    private static final String OUTCOME_SUPPRESSED = "suppressed";
    private static final String OUTCOME_FALLBACK = "full_fallback";

    private final SoakStreamSubscriberFactory subscribers;
    private final NearCacheAdmin nearCacheAdmin;
    private final StreamingSoakConfig cfg;
    private final boolean counterStreamsTwice;
    private final String cachePrefix;
    private final SoakReport report;
    private final Random rnd;
    private final long opTimeoutNanos;
    private final VariantMatrix matrix;
    private final GatewayCommandDriver driver;
    private final StreamVerifier verifier;
    private final SoakPayloads payloads;
    private final Map<String, Long> nearPatchOutcomes = new TreeMap<>();

    // Size-aware keys, derived once from the payload generator. The subscribed/other keys live inside the
    // k0..kN whole-cache range; the delta/suppress/fallback keys are kept outside it so the random overwrite
    // phase never touches them.
    private final String keySubscribed;
    private final String keyOther;
    private final String keyDelta;
    private final String keySuppress;
    private final String keyFallback;

    private long cacheSeq;
    private String currentCell = "";

    /**
     * @param counterStreamsTwice whether a counter mutation streams two identical ADD_ITEMs to a subscriber (the
     *                            cluster reuses the counter result as the broadcast). Transport-determined - true
     *                            for the binary Aeron transport, false for the one-directional URL transports.
     * @param cachePrefix         a per-transport cache-id prefix so concurrent runs never collide on ids.
     */
    public StreamingSoakWorkload(GatewayClient client, SoakRecordingListener listener,
                                 SoakStreamSubscriberFactory subscribers, NearCacheAdmin nearCacheAdmin,
                                 StreamingSoakConfig cfg, boolean counterStreamsTwice, String cachePrefix) {
        this.subscribers = subscribers;
        this.nearCacheAdmin = nearCacheAdmin;
        this.cfg = cfg;
        this.counterStreamsTwice = counterStreamsTwice;
        this.cachePrefix = cachePrefix;
        this.report = new SoakReport(cfg.seed, cfg.durationSeconds, cfg.toConfigMap());
        this.rnd = new Random(cfg.seed);
        this.opTimeoutNanos = TimeUnit.SECONDS.toNanos(cfg.opTimeoutSeconds);
        this.matrix = new VariantMatrix(cfg.includeCounters, cfg.includeSynthetic, rnd);
        this.driver = new GatewayCommandDriver(client, listener, this, opTimeoutNanos, cfg.opTimeoutSeconds);
        this.verifier = new StreamVerifier(driver, this, report, opTimeoutNanos, cfg.opTimeoutSeconds);
        this.payloads = new SoakPayloads(cfg.keySizeBytes, cfg.valueSizeBytes);
        this.keySubscribed = payloads.key("0");
        this.keyOther = payloads.key("1");
        this.keyDelta = payloads.key("d");
        this.keySuppress = payloads.key("s");
        this.keyFallback = payloads.key("f");
    }

    @Override
    public SoakReport report() {
        return report;
    }

    @Override
    public void run() {
        log.info("Starting streaming soak run with {} ({} matrix cells)", cfg, matrix.size());

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.durationSeconds);
        long rounds = 0;
        // Round-robin through the (seeded-shuffled) matrix so the first full pass covers every cell.
        while (System.nanoTime() < deadline) {
            var variant = matrix.at(rounds);
            runRound(variant);
            matrix.recordCovered(variant);
            report.recordCount("round");
            rounds++;

            if (rounds % PROGRESS_EVERY == 0) {
                report.sampleHeap();
                driver.checkHealth();
                log.info(report.progressLine());
            }
        }

        report.sampleHeap();
        report.putSection("coverage", matrix.coverage());
        if (!nearPatchOutcomes.isEmpty()) {
            report.putSection("nearPatch", nearPatchOutcomes);
        }

        // A green run must prove full matrix coverage - but only once at least one full pass completed (a very
        // short run may legitimately not reach every cell).
        if (rounds >= matrix.size()) {
            var gap = matrix.firstUncoveredCell();
            if (gap != null) {
                throw fail("coverage gap: matrix cell never exercised: " + gap);
            }
            log.info("Streaming soak complete: all {} matrix cells covered across {} rounds. {}",
                    matrix.size(), rounds, report.progressLine());
        } else {
            log.warn("Streaming soak complete but only {} rounds ran (< {} cells); coverage is partial. {}",
                    rounds, matrix.size(), report.progressLine());
        }
    }

    // ---------------------------------------------------------------- round

    private void runRound(Variant v) {
        currentCell = v.cell();
        if (v.warm()) {
            runWarmRound(v);
            return;
        }
        var caches = createCaches(v);
        var model = new RoundModel();
        var expected = new Expectations(caches);
        var deleted = new ArrayList<String>();

        // Pre-subscribe seeding serves two purposes: it is the hydration source for full-mode rounds, and it
        // gives counter rounds a defined base for inc/dec/set. Entries created before subscribe do not stream
        // live; with hydration they are replayed as ADD_ITEMs (verified and consumed below). Snapshot hydration
        // is NOT key-filtered by the gateway (accepted quirk): it replays every entry present at subscribe time,
        // so every seeded key is expected regardless of the subscription's key filter.
        var seedKeys = computeSeedKeys(v);
        boolean willHydrate = v.hydrate() && v.mode() == Mode.FULL;
        var hydrationExpected = new Expectations(caches);
        for (var cache : caches) {
            for (var key : seedKeys) {
                String valueStr;
                if (v.counters()) {
                    long init = rnd.nextInt(1_000);
                    driver.addCounterEntry(cache, key, init);
                    model.putCounter(cache, key, init);
                    valueStr = Long.toString(init);
                } else {
                    var value = regularValue();
                    driver.addEntry(cache, key, value);
                    model.putRegular(cache, key, value);
                    valueStr = value;
                }
                if (willHydrate) {
                    hydrationExpected.add(cache, new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, valueStr));
                }
            }
        }

        var subscriber = subscribers.open(toSpec(v, caches));
        try {
            subscriber.awaitAck(opTimeoutNanos);

            if (willHydrate && !seedKeys.isEmpty()) {
                verifier.verifyHydration(subscriber, hydrationExpected);
            }

            if (v.mode() == Mode.PATCH) {
                buildPatchLive(v, caches, model, expected);
            } else {
                buildFullLive(v, caches, model, expected, deleted);
            }

            verifier.verifyLiveOrdered(subscriber, expected);
            report.recordVerification();

            // Binary transport only: an explicit unsubscribe + probe mutation proves the stream falls silent. The
            // URL transports cannot (closing races the server's disconnect), so there the capability is absent.
            probeSilenceAfterUnsubscribe(subscriber, v, caches, deleted);
        } finally {
            subscriber.close();
        }

        cleanup(v, caches, deleted);
    }

    // ---------------------------------------------------------------- live mutation sequences

    private void buildFullLive(Variant v, List<String> caches, RoundModel model, Expectations expected,
                               List<String> deleted) {
        var keys = keysFor(v);
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            if (v.counters()) {
                fullCounterMutation(v, cache, key, model, expected);
            } else {
                fullRegularMutation(v, cache, key, model, expected);
            }
        }

        // Whole-cache subscribers also observe cache-wide lifecycle events. Exercise CLEAR_CACHE and DELETE_CACHE
        // on regular caches only - counter cache-wide events aren't asserted here (counter correctness is covered
        // by the core soak); counter caches are torn down silently in cleanup after the unsubscribe.
        if (v.whole() && !v.counters()) {
            var clearCache = caches.get(rnd.nextInt(caches.size()));
            driver.clearCache(clearCache, false);
            model.clear(clearCache);
            expected.add(clearCache, new ExpectedEvent(clearCache, StreamEvent.CLEAR_CACHE, "", ""));

            var deleteCache = caches.get(caches.size() - 1);
            driver.deleteCache(deleteCache, false);
            expected.add(deleteCache, new ExpectedEvent(deleteCache, StreamEvent.DELETE_CACHE, "", ""));
            deleted.add(deleteCache);
        }
    }

    /**
     * Keys to seed before subscribing: counter rounds always (a base for inc/dec/set), regular full-mode rounds
     * only when hydrating (to create a hydration source). Regular patch rounds never pre-seed - their own
     * first-add phase (post-subscribe) is what a patch subscriber must not be notified of.
     */
    private List<String> computeSeedKeys(Variant v) {
        if (v.counters()) {
            return keysFor(v);
        }
        if (v.mode() == Mode.FULL && v.hydrate()) {
            // Only seed keys the subscription can see. Snapshot hydration is NOT key-filtered by the gateway (it
            // replays every entry present at subscribe time), so seeding a non-subscribed key would make
            // hydration deliver it too. Seeding only visible keys keeps the hydration expectation exact; live
            // key-filtering is still exercised by introducing the non-subscribed key after subscribe.
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

    private void fullRegularMutation(Variant v, String cache, String key, RoundModel model, Expectations expected) {
        boolean present = model.containsRegular(cache, key);
        if (present && rnd.nextInt(4) == 0) {
            driver.removeEntry(cache, key);
            model.removeRegular(cache, key);
            if (visible(v, key)) {
                expected.add(cache, new ExpectedEvent(cache, StreamEvent.REMOVE_ITEM, key, ""));
            }
        } else {
            var value = regularValue();
            driver.addEntry(cache, key, value);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.add(cache, new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, value));
            }
        }
    }

    private void fullCounterMutation(Variant v, String cache, String key, RoundModel model, Expectations expected) {
        // Keys are pre-seeded before subscribe, so only the confirmed streaming mutations run here: increment /
        // decrement / set all stream the new value as an ADD_ITEM.
        long current = model.counter(cache, key);
        long newValue;
        int choice = rnd.nextInt(3);
        if (choice == 0) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current + delta;
            driver.incrementCounter(cache, key, delta);
        } else if (choice == 1) {
            long delta = rnd.nextInt(10) + 1;
            newValue = current - delta;
            driver.decrementCounter(cache, key, delta);
        } else {
            newValue = rnd.nextInt(1_000);
            driver.setCounter(cache, key, newValue);
        }
        model.putCounter(cache, key, newValue);
        if (visible(v, key)) {
            var event = new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, Long.toString(newValue));
            expected.add(cache, event);
            if (counterStreamsTwice) {
                // Accepted quirk (binary transport): the gateway delivers each counter update to a session that
                // both mutates and subscribes TWICE - once as the command result, once as the subscriber
                // broadcast (the cluster reuses the single counter-result message as the broadcast). The two
                // copies arrive consecutively per cache, so model exactly two identical ADD_ITEMs.
                expected.add(cache, event);
            }
        }
    }

    private void buildPatchLive(Variant v, List<String> caches, RoundModel model, Expectations expected) {
        var keys = keysFor(v);
        // Phase 1: first-add every key. A PATCH-mode subscriber present for the first insert of a key is notified
        // with the whole value as a full ADD_ITEM baseline (there is no prior value to diff against), so a
        // subscriber that joined before the key existed is not left holding a stale (empty) view.
        for (var cache : caches) {
            for (var key : keys) {
                addFirst(v, cache, key, model, expected);
            }
        }
        // Phase 2: overwrite existing keys -> each streams a PATCH_ITEM carrying the delta (== new value here).
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            overwriteDelta(v, cache, key, model, expected);
        }
        // Phase 3: deterministic coverage of the two remaining patch outcomes on dedicated keys (kept outside the
        // random key range so Phase 2 never collides with them): a no-op overwrite that is SUPPRESSED (no event
        // at all), and an overwrite whose value is not parseable JSON, which falls back to a full ADD_ITEM.
        appendPatchOutcomeTail(v, caches, model, expected);
    }

    /**
     * Append the suppressed and full-fallback patch outcomes. In whole-cache scope they run on their own
     * dedicated keys (each first-added, which itself emits a baseline ADD_ITEM); in single-key scope only the
     * subscribed key is visible, so they run in sequence on it (it already exists from Phase 1).
     */
    private void appendPatchOutcomeTail(Variant v, List<String> caches, RoundModel model, Expectations expected) {
        for (var cache : caches) {
            if (v.whole()) {
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
     * A round that exercises near-cache synthetic patch generation on the gateway edge: seed a regular cache,
     * prime it so the warm local copy populates, then open a hydrating PATCH subscription served from the warm
     * copy. The subscription receives a full ADD_ITEM baseline per warm key on hydration, then one synthetic event
     * per live mutation - a PATCH_ITEM delta or a full ADD_ITEM fallback (unparseable value), and nothing for a
     * no-op (suppressed) overwrite. These are verified in order with no extras, and the per-outcome patch counter
     * is asserted to have advanced by exactly the expected amount, proving the deltas were synthesised from the
     * warm copy rather than taken on the cluster path.
     */
    private void runWarmRound(Variant v) {
        var caches = createCaches(v);
        var model = new RoundModel();
        var seedKeys = warmSeedKeys(v);

        for (var cache : caches) {
            for (var key : seedKeys) {
                var value = regularValue();
                driver.addEntry(cache, key, value);
                model.putRegular(cache, key, value);
            }
            nearCacheAdmin.prime(cache);
            nearCacheAdmin.awaitWarm(cache, seedKeys.size(), opTimeoutNanos);
        }

        var expected = new Expectations(caches);
        var hydrationExpected = new Expectations(caches);
        for (var cache : caches) {
            for (var key : seedKeys) {
                if (visible(v, key)) {
                    hydrationExpected.add(cache, new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, model.regular(cache, key)));
                }
            }
        }

        var subscriber = subscribers.open(toSpec(v, caches));
        try {
            subscriber.awaitAck(opTimeoutNanos);
            verifier.verifyHydration(subscriber, hydrationExpected);

            boolean assertMetrics = nearCacheAdmin.supportsPatchMetrics();
            var before = assertMetrics ? snapshotOutcomes() : null;
            var increments = new TreeMap<String, Long>();
            for (var cache : caches) {
                if (v.whole()) {
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

            verifier.verifyLiveOrdered(subscriber, expected);
            report.recordVerification();

            if (assertMetrics) {
                increments.forEach((outcome, delta) -> {
                    awaitPatchCount(outcome, before.get(outcome) + delta);
                    nearPatchOutcomes.merge(outcome, delta, Long::sum);
                });
                report.recordVerification();
            }
        } finally {
            // Binary transport: unsubscribe the (regular) caches explicitly; URL transports: close() unsubscribes.
            var probe = subscriber.liveUnsubscribeProbe();
            if (probe != null) {
                probe.unsubscribe(caches, v.counters());
            }
            subscriber.close();
        }

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
    private void addFirst(Variant v, String cache, String key, RoundModel model, Expectations expected) {
        var value = regularValue();
        driver.addEntry(cache, key, value);
        model.putRegular(cache, key, value);
        if (visible(v, key)) {
            expected.add(cache, new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, value));
        }
    }

    /** Overwrite an existing key with a changed value; a visible overwrite streams a PATCH_ITEM delta (== the new
     * value for single-field {@code {"v":N}} values). */
    private void overwriteDelta(Variant v, String cache, String key, RoundModel model, Expectations expected) {
        var value = regularValueDiffering(model.regular(cache, key));
        driver.addEntry(cache, key, value);
        model.putRegular(cache, key, value);
        if (visible(v, key)) {
            expected.add(cache, new ExpectedEvent(cache, StreamEvent.PATCH_ITEM, key, value));
        }
    }

    /** Re-add an existing key with its current value; the merge patch is empty so the update is SUPPRESSED (no
     * event is streamed). The model value is unchanged. */
    private void overwriteSuppressed(String cache, String key, RoundModel model) {
        driver.addEntry(cache, key, model.regular(cache, key));
    }

    /** Overwrite an existing key with a value that is not parseable JSON; no merge patch can be built, so the
     * whole value is streamed as a full ADD_ITEM fallback rather than nothing. */
    private void overwriteFallback(Variant v, String cache, String key, RoundModel model, Expectations expected) {
        driver.addEntry(cache, key, UNPARSEABLE_VALUE);
        model.putRegular(cache, key, UNPARSEABLE_VALUE);
        if (visible(v, key)) {
            expected.add(cache, new ExpectedEvent(cache, StreamEvent.ADD_ITEM, key, UNPARSEABLE_VALUE));
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
        return v.whole() || keySubscribed.equals(key);
    }

    // ---------------------------------------------------------------- subscription lifecycle

    private List<String> createCaches(Variant v) {
        int n = v.multi() ? 2 : 1;
        var ids = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            var id = cachePrefix + (++cacheSeq);
            driver.createCache(id, v.counters());
            ids.add(id);
        }
        return ids;
    }

    private void cleanup(Variant v, List<String> caches, List<String> deleted) {
        for (var cache : caches) {
            if (deleted.contains(cache)) {
                continue;
            }
            driver.deleteCache(cache, v.counters());
        }
    }

    private SubscriptionSpec toSpec(Variant v, List<String> caches) {
        boolean keyScoped = v.scope() == Scope.KEY;
        return new SubscriptionSpec(List.copyOf(caches), keyScoped, keyScoped ? keySubscribed : null,
                v.mode() == Mode.PATCH, v.hydrate(), v.counters());
    }

    /**
     * Binary transport only (the subscriber exposes {@link LiveUnsubscribeProbe}): unsubscribe the still-alive
     * caches, issue a mutation that <em>would</em> stream to this subscription if it were still live, then prove
     * silence. For the URL transports the probe is absent and this is a no-op.
     */
    private void probeSilenceAfterUnsubscribe(SoakStreamSubscriber subscriber, Variant v, List<String> caches,
                                              List<String> deleted) {
        var probe = subscriber.liveUnsubscribeProbe();
        if (probe == null) {
            return;
        }
        var active = new ArrayList<String>();
        caches.forEach(c -> {
            if (!deleted.contains(c)) {
                active.add(c);
            }
        });
        if (active.isEmpty()) {
            return;
        }
        probe.unsubscribe(active, v.counters());

        var probeCache = active.get(0);
        // Whole-cache scope: k0 is the first whole-cache key, which is the subscribed key.
        var probeKey = keySubscribed;
        if (v.counters()) {
            // Increment (not add) a pre-seeded key: a mutation that definitely streams if still subscribed.
            driver.incrementCounter(probeCache, probeKey, 1L);
        } else {
            driver.addEntry(probeCache, probeKey, regularValue());
        }
        verifier.assertSilence(subscriber, Set.copyOf(active), "received stream event after unsubscribe");
        report.recordCount("unsubscribeNegativeChecked");
    }

    // ---------------------------------------------------------------- misc helpers

    @Override
    public AssertionError fail(String detail) {
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
}
