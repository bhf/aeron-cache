package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import io.aeron.Aeron;
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
import java.util.function.LongSupplier;

/**
 * The shared streaming soak workload for the URL-based gateways (streaming WebSocket and SSE): a coverage-driven,
 * time-bounded driver that verifies the gateway's subscription/streaming behaviour across the full matrix of
 * subscription variations - cache kind (regular/counter), mode (full/patch), scope (whole-cache/single-key),
 * snapshot hydration and cardinality (single/multi-cache).
 *
 * <p><b>Split roles.</b> Mutations are applied over the binary Aeron {@link GatewayClient} (the in-process
 * mutation driver), while the subscription under test is observed over the URL transport via an injected
 * {@link SoakStreamSubscriberFactory} - the one seam that differs between the WS and SSE suites. Each round opens
 * a fresh subscriber for the variant's caches (the subscription is encoded in the connect URL), verifies
 * snapshot hydration if requested, applies a mutation sequence while computing the exact events the subscription
 * must receive, asserts the received stream equals that expectation in order with no extras, then closes the
 * subscriber (unsubscribe) and checks the stream falls silent.
 *
 * <p>The matrix is cycled round-robin in a seeded-shuffled order so the first full pass exercises every cell;
 * per-cell counts are written to the report and the run fails if any cell was missed once a full pass completed,
 * so a green run proves full coverage. Values are single-field JSON {@code {"v":N}} so merge-patch == overwrite.
 */
public final class UrlStreamingSoakWorkload implements SoakRun {

    enum Kind { REGULAR, COUNTER }

    enum Mode { FULL, PATCH }

    enum Scope { WHOLE, KEY }

    enum Card { SINGLE, MULTI }

    record Variant(Kind kind, Mode mode, Scope scope, boolean hydrate, Card card) {
        String cell() {
            return kind + "/" + mode + "/" + scope + "/" + (hydrate ? "hydrate" : "nohydrate") + "/" + card;
        }
    }

    record ExpectedEvent(String cacheId, String type, String key, String value) {
        boolean matches(StreamEvent u) {
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

    private static final Logger log = LogManager.getLogger(UrlStreamingSoakWorkload.class);

    private static final String ADD_ITEM = "ADD_ITEM";
    private static final String PATCH_ITEM = "PATCH_ITEM";
    private static final String REMOVE_ITEM = "REMOVE_ITEM";
    private static final String CLEAR_CACHE = "CLEAR_CACHE";
    private static final String DELETE_CACHE = "DELETE_CACHE";

    private static final long TTL_NONE = 0L;
    private static final long PARK_NANOS = 50_000L;
    private static final long GRACE_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
    private static final long PROGRESS_EVERY = 50L;
    private static final String KEY_SUBSCRIBED = "k0";
    private static final String KEY_OTHER = "k1";

    private final GatewayClient client;
    private final SoakRecordingListener listener;
    private final SoakStreamSubscriberFactory subscribers;
    private final UrlStreamingSoakConfig cfg;
    private final boolean counterStreamsTwice;
    private final SoakReport report;
    private final Random rnd;
    private final long opTimeoutNanos;
    private final List<Variant> matrix;
    private final Map<String, Long> coverage = new TreeMap<>();
    private final String cachePrefix;

    private long corrSeq;
    private long cacheSeq;
    private String currentCell = "";

    /**
     * @param counterStreamsTwice whether a counter mutation streams two identical ADD_ITEMs to a subscriber (the
     *                            cluster reuses the counter result as the broadcast). Transport-determined - set
     *                            from an observed short run.
     * @param cachePrefix         a per-transport cache-id prefix so WS and SSE runs never collide on ids.
     */
    public UrlStreamingSoakWorkload(GatewayClient client, SoakRecordingListener listener,
                                    SoakStreamSubscriberFactory subscribers, UrlStreamingSoakConfig cfg,
                                    boolean counterStreamsTwice, String cachePrefix) {
        this.client = client;
        this.listener = listener;
        this.subscribers = subscribers;
        this.cfg = cfg;
        this.counterStreamsTwice = counterStreamsTwice;
        this.cachePrefix = cachePrefix;
        this.report = new SoakReport(cfg.seed, cfg.durationSeconds, cfg.toConfigMap());
        this.rnd = new Random(cfg.seed);
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
        log.info("Starting URL streaming soak run with {} ({} matrix cells)", cfg, matrix.size());

        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(cfg.durationSeconds);
        int index = 0;
        long rounds = 0;
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

        if (rounds >= matrix.size()) {
            for (var variant : matrix) {
                if (coverage.getOrDefault(variant.cell(), 0L) == 0L) {
                    throw fail("coverage gap: matrix cell never exercised: " + variant.cell());
                }
            }
            log.info("URL streaming soak complete: all {} matrix cells covered across {} rounds. {}",
                    matrix.size(), rounds, report.progressLine());
        } else {
            log.warn("URL streaming soak complete but only {} rounds ran (< {} cells); coverage is partial. {}",
                    rounds, matrix.size(), report.progressLine());
        }
    }

    private static List<Variant> buildMatrix(boolean includeCounters) {
        var variants = new ArrayList<Variant>();
        var kinds = includeCounters ? List.of(Kind.REGULAR, Kind.COUNTER) : List.of(Kind.REGULAR);
        for (var kind : kinds) {
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
        var caches = createCaches(v);
        var model = new RoundModel();
        var expected = new HashMap<String, List<ExpectedEvent>>();
        caches.forEach(c -> expected.put(c, new ArrayList<>()));
        var deleted = new ArrayList<String>();

        // Pre-subscribe seeding: the hydration source for full-mode rounds, and a defined base for counter
        // inc/dec/set. Entries created before subscribe do not stream live; with hydration they are replayed.
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
                    sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "seed addEntry " + cache + "/" + key);
                    model.putRegular(cache, key, value);
                    valueStr = value;
                }
                if (willHydrate) {
                    hydrationExpected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, valueStr));
                }
            }
        }

        var subscriber = subscribers.open(toSpec(v, caches));
        try {
            subscriber.awaitAck(opTimeoutNanos);

            if (willHydrate && !seedKeys.isEmpty()) {
                verifyHydration(subscriber, hydrationExpected);
            }

            if (v.mode() == Mode.PATCH) {
                buildPatchLive(v, caches, model, expected);
            } else {
                buildFullLive(v, caches, model, expected, deleted);
            }

            verifyLiveOrdered(subscriber, expected);
            report.recordVerification();
        } finally {
            // Unsubscribe == closing the connection for the URL transports. The negative assertion (no spurious
            // events for a live subscription) is the grace-window check inside collect(); a post-close probe
            // would race the server's disconnect handling, so it is deliberately not attempted here.
            subscriber.close();
        }

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

        if (v.scope() == Scope.WHOLE && v.kind() == Kind.REGULAR) {
            var clearCache = caches.get(rnd.nextInt(caches.size()));
            sendCommand(c -> client.clearCache(c, clearCache), "clearCache " + clearCache);
            model.clear(clearCache);
            expected.get(clearCache).add(new ExpectedEvent(clearCache, CLEAR_CACHE, "", ""));

            var deleteCache = caches.get(caches.size() - 1);
            sendCommand(c -> client.deleteCache(c, deleteCache), "deleteCache " + deleteCache);
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
            sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "addEntry " + cache + "/" + key);
            model.putRegular(cache, key, value);
            if (visible(v, key)) {
                expected.get(cache).add(new ExpectedEvent(cache, ADD_ITEM, key, value));
            }
        }
    }

    private void fullCounterMutation(Variant v, String cache, String key, RoundModel model,
                                     Map<String, List<ExpectedEvent>> expected) {
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
            final long set = newValue;
            sendCommand(c -> client.setCounter(c, cache, key, set, TTL_NONE), "setCounter " + cache + "/" + key);
        }
        model.putCounter(cache, key, newValue);
        if (visible(v, key)) {
            var event = new ExpectedEvent(cache, ADD_ITEM, key, Long.toString(newValue));
            expected.get(cache).add(event);
            if (counterStreamsTwice) {
                // The cluster reuses the single counter result as the subscriber broadcast, so a self-mutating
                // or cross-session subscriber receives two identical consecutive ADD_ITEMs per update.
                expected.get(cache).add(event);
            }
        }
    }

    private void buildPatchLive(Variant v, List<String> caches, RoundModel model,
                                Map<String, List<ExpectedEvent>> expected) {
        var keys = keysFor(v);
        // Phase 1: first-add every key. A PATCH-mode subscriber is NOT notified on first add (no expected event).
        for (var cache : caches) {
            for (var key : keys) {
                var value = regularValue();
                sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "patch first-add " + cache + "/" + key);
                model.putRegular(cache, key, value);
            }
        }
        // Phase 2: overwrite existing keys -> each streams a PATCH_ITEM carrying the delta (== new value here).
        for (int step = 0; step < cfg.mutationsPerRound; step++) {
            var cache = caches.get(rnd.nextInt(caches.size()));
            var key = keys.get(rnd.nextInt(keys.size()));
            var value = regularValue();
            sendCommand(c -> client.addEntry(c, cache, key, value, TTL_NONE), "patch overwrite " + cache + "/" + key);
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

    private void verifyHydration(SoakStreamSubscriber subscriber, Map<String, List<ExpectedEvent>> hydrationExpected) {
        int totalExpected = hydrationExpected.values().stream().mapToInt(List::size).sum();
        var got = collect(subscriber, hydrationExpected.keySet(), totalExpected, "hydration");
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

    private void verifyLiveOrdered(SoakStreamSubscriber subscriber, Map<String, List<ExpectedEvent>> expected) {
        int totalExpected = expected.values().stream().mapToInt(List::size).sum();
        var got = collect(subscriber, expected.keySet(), totalExpected, "live stream");
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
                    throw fail("timed out after " + cfg.opTimeoutSeconds + "s awaiting " + what
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
                throw fail(what + ": unexpected extra stream event after collecting " + count + "/" + totalExpected
                        + " " + renderAll(got) + ": " + u);
            }
        }
        return got;
    }

    // ---------------------------------------------------------------- mutations (gateway client + await)

    private void removeRegular(String cache, String key) {
        var corr = nextCorr();
        sendWithRetry(() -> client.removeEntry(corr, cache, key));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS && r.status() != OperationStatus.UNKNOWN_KEY) {
            throw fail("removeEntry " + cache + "/" + key + " unexpected status " + r.status());
        }
    }

    private List<String> createCaches(Variant v) {
        int n = v.card() == Card.MULTI ? 2 : 1;
        var ids = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            var id = cachePrefix + (++cacheSeq);
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
            if (v.kind() == Kind.COUNTER) {
                sendCommand(c -> client.deleteCounterCache(c, cache), "deleteCounterCache " + cache);
            } else {
                sendCommand(c -> client.deleteCache(c, cache), "deleteCache " + cache);
            }
        }
    }

    private SubscriptionSpec toSpec(Variant v, List<String> caches) {
        boolean keyScoped = v.scope() == Scope.KEY;
        return new SubscriptionSpec(List.copyOf(caches), keyScoped, keyScoped ? KEY_SUBSCRIBED : null,
                v.mode() == Mode.PATCH, v.hydrate(), v.kind() == Kind.COUNTER);
    }

    // ---------------------------------------------------------------- await + health helpers

    private void sendCommand(CorrLongSupplier op, String what) {
        var corr = nextCorr();
        sendWithRetry(() -> op.send(corr));
        var r = awaitCommand(corr);
        if (r.status() != OperationStatus.SUCCESS) {
            throw fail(what + " expected SUCCESS but got " + r.status());
        }
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

    private void checkHealth(SoakStreamSubscriber subscriber) {
        checkHealth();
        if (!subscriber.isHealthy()) {
            throw fail("streaming subscriber connection failed during soak run");
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

    private static String sig(ExpectedEvent e) {
        return e.type() + "|" + e.key() + "|" + e.value();
    }

    private static String sig(StreamEvent u) {
        return switch (u.eventType()) {
            case ADD_ITEM, PATCH_ITEM -> u.eventType() + "|" + u.key() + "|" + u.value();
            case REMOVE_ITEM -> u.eventType() + "|" + u.key() + "|";
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

    /** A command send that needs the per-call correlation id (so the workload can await the response). */
    @FunctionalInterface
    private interface CorrLongSupplier {
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
