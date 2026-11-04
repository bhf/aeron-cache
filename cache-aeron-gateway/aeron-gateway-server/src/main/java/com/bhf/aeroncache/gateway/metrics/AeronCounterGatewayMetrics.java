package com.bhf.aeroncache.gateway.metrics;

import io.aeron.Aeron;
import io.aeron.Counter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link GatewayMetrics} backed by counters in the Aeron media driver.
 * <p>
 * Registers:
 * <ul>
 *     <li>{@code SessionsConnected} - the number of clients currently connected (a gauge).</li>
 *     <li>{@code SessionsTotal} - the number of client connections since the gateway started.</li>
 *     <li>{@code StreamUpdatesPublished} - streaming subscription updates sent to clients.</li>
 *     <li>One {@code Requests-<type>} counter per {@link GatewayRequestType}.</li>
 *     <li>Per-domain subscription gauges ({@code Subscriptions-<domain>-whole/-keyed}, {@code Subscribers-<domain>}).</li>
 *     <li>Per-domain near cache gauges ({@code NearWarmCaches-<domain>}, {@code NearSubscribers-<domain>-whole/-keyed},
 *     {@code NearPatchSubscribers-<domain>}), hit counters ({@code NearHydrationsServed-<domain>-whole/-keyed})
 *     and synthetic patch counters ({@code NearPatches-<domain>-delta/-baseline/-suppressed}), registered only
 *     when the near cache is enabled.</li>
 * </ul>
 * The session counters are updated from the Aeron conductor thread so use atomic updates. Request counters are
 * written only by the gateway ingress thread, and the update counter only by the cluster egress thread, so each
 * uses an ordered increment.
 */
@Log4j2
public class AeronCounterGatewayMetrics implements GatewayMetrics {
    /**
     * Type id of the counters registered by this class. Values below 1000 are reserved by Aeron.
     */
    public static final int COUNTER_TYPE_ID = 2100;

    private static final String LABEL_PREFIX = "AeronCache-Gateway-";

    private final Aeron aeron;
    private final String suffix;
    private final Counter sessionsConnected;
    private final Counter sessionsTotal;
    private final Counter streamUpdatesPublished;
    private final Counter[] requestCounters;

    /**
     * Per-domain subscription gauges, registered lazily on the first {@link #setSubscriptions} call for a
     * domain. One set of counters (whole, keyed, subscribers) per cache domain (cache / counters).
     */
    private final Map<String, SubscriptionGauges> subscriptionGauges = new ConcurrentHashMap<>();

    private record SubscriptionGauges(Counter whole, Counter keyed, Counter subscribers) {
    }

    /**
     * Per-domain near cache gauges, registered lazily on the first {@link #setNearCache} call for a domain.
     * One set of counters (warm caches, whole-cache subscribers, keyed subscribers) per cache domain.
     */
    private final Map<String, NearCacheGauges> nearCacheGauges = new ConcurrentHashMap<>();

    /**
     * Per-domain near cache counters. {@code warmCaches} / {@code wholeSubscribers} / {@code keyedSubscribers}
     * / {@code patchSubscribers} are gauges set via {@link #setNearCache}; {@code wholeHydrations} /
     * {@code keyedHydrations} are monotonic hit counters incremented via {@link #onNearCacheHydrationServed};
     * {@code patches} holds the synthetic patch counters indexed by {@link NearPatchOutcome#ordinal()},
     * incremented via {@link #onNearPatch}. All are registered together so the counters are allocated at
     * startup on the main thread, not lazily from the egress thread.
     */
    private record NearCacheGauges(Counter warmCaches, Counter wholeSubscribers, Counter keyedSubscribers,
                                   Counter patchSubscribers, Counter wholeHydrations, Counter keyedHydrations,
                                   Counter[] patches) {
    }

    /**
     * @param aeron  client connected to the media driver the counters are registered with.
     * @param nodeId identifier for this gateway, included in the counter labels.
     */
    public AeronCounterGatewayMetrics(Aeron aeron, String nodeId) {
        this.aeron = aeron;
        final String suffix = "-" + nodeId;
        this.suffix = suffix;
        sessionsConnected = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "SessionsConnected" + suffix);
        sessionsTotal = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "SessionsTotal" + suffix);
        streamUpdatesPublished = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "StreamUpdatesPublished" + suffix);
        requestCounters = new Counter[GatewayRequestType.values().length];
        for (GatewayRequestType type : GatewayRequestType.values()) {
            requestCounters[type.ordinal()] =
                    aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "Requests-" + type.label() + suffix);
        }
        log.info("Registered {} Aeron gateway metrics counters for {}", requestCounters.length + 3, nodeId);
    }

    @Override
    public void onSessionConnected() {
        sessionsConnected.increment();
        sessionsTotal.increment();
    }

    @Override
    public void onSessionDisconnected() {
        sessionsConnected.decrement();
    }

    @Override
    public void onRequest(GatewayRequestType requestType) {
        requestCounters[requestType.ordinal()].incrementOrdered();
    }

    @Override
    public void onStreamUpdatePublished() {
        streamUpdatesPublished.incrementOrdered();
    }

    @Override
    public void setSubscriptions(String domain, long wholeCacheSubscriptions, long keyedSubscriptions, long subscribers) {
        final SubscriptionGauges gauges = subscriptionGauges.computeIfAbsent(domain, this::registerSubscriptionGauges);
        gauges.whole().setOrdered(wholeCacheSubscriptions);
        gauges.keyed().setOrdered(keyedSubscriptions);
        gauges.subscribers().setOrdered(subscribers);
    }

    private SubscriptionGauges registerSubscriptionGauges(String domain) {
        final Counter whole = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "Subscriptions-" + domain + "-whole" + suffix);
        final Counter keyed = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "Subscriptions-" + domain + "-keyed" + suffix);
        final Counter subscribers = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "Subscribers-" + domain + suffix);
        log.info("Registered gateway subscription gauges for {} domain", domain);
        return new SubscriptionGauges(whole, keyed, subscribers);
    }

    @Override
    public void setNearCache(String domain, long warmCaches, long wholeCacheSubscribers, long keyedSubscribers,
                             long patchSubscribers) {
        final NearCacheGauges gauges = nearCacheGauges.computeIfAbsent(domain, this::registerNearCacheGauges);
        gauges.warmCaches().setOrdered(warmCaches);
        gauges.wholeSubscribers().setOrdered(wholeCacheSubscribers);
        gauges.keyedSubscribers().setOrdered(keyedSubscribers);
        gauges.patchSubscribers().setOrdered(patchSubscribers);
    }

    @Override
    public void onNearCacheHydrationServed(String domain, boolean keyed) {
        final NearCacheGauges gauges = nearCacheGauges.computeIfAbsent(domain, this::registerNearCacheGauges);
        (keyed ? gauges.keyedHydrations() : gauges.wholeHydrations()).incrementOrdered();
    }

    @Override
    public void onNearPatch(String domain, NearPatchOutcome outcome) {
        final NearCacheGauges gauges = nearCacheGauges.computeIfAbsent(domain, this::registerNearCacheGauges);
        gauges.patches()[outcome.ordinal()].incrementOrdered();
    }

    private NearCacheGauges registerNearCacheGauges(String domain) {
        final Counter warmCaches = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearWarmCaches-" + domain + suffix);
        final Counter wholeSubscribers = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearSubscribers-" + domain + "-whole" + suffix);
        final Counter keyedSubscribers = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearSubscribers-" + domain + "-keyed" + suffix);
        final Counter patchSubscribers = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearPatchSubscribers-" + domain + suffix);
        final Counter wholeHydrations = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearHydrationsServed-" + domain + "-whole" + suffix);
        final Counter keyedHydrations = aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearHydrationsServed-" + domain + "-keyed" + suffix);
        final Counter[] patches = new Counter[NearPatchOutcome.values().length];
        for (final NearPatchOutcome outcome : NearPatchOutcome.values()) {
            patches[outcome.ordinal()] =
                    aeron.addCounter(COUNTER_TYPE_ID, LABEL_PREFIX + "NearPatches-" + domain + "-" + outcome.label() + suffix);
        }
        log.info("Registered gateway near cache gauges for {} domain", domain);
        return new NearCacheGauges(warmCaches, wholeSubscribers, keyedSubscribers, patchSubscribers, wholeHydrations,
                keyedHydrations, patches);
    }
}
