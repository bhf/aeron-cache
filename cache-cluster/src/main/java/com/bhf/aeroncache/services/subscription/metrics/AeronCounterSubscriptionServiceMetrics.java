package com.bhf.aeroncache.services.subscription.metrics;

import io.aeron.Aeron;
import io.aeron.Counter;
import lombok.extern.log4j.Log4j2;

/**
 * A {@link SubscriptionServiceMetrics} backed by counters in the Aeron media driver, in the style of
 * {@link com.bhf.aeroncache.services.tracing.impl.AeronCounterTracingService}.
 * <p>
 * One instance is registered per subscription type (cache / patch / counters); the type is embedded in
 * the counter labels so the three subscription services on a node never collide. Registers:
 * <ul>
 *     <li>One {@code <Event>} counter per {@link Event} (subscriptions, unsubscriptions, duplicates,
 *     session closes and published updates).</li>
 *     <li>An {@code Active} gauge holding the current number of active subscriptions.</li>
 * </ul>
 * All counters for a node are written only by the cluster service's single processing thread, so the
 * event counters use ordered increments and the gauge an ordered set. The counters can be viewed with
 * tools that read the media driver's counters file (e.g. AeronStat) and are exported in Prometheus
 * format by the clustertools sidecar.
 */
@Log4j2
public class AeronCounterSubscriptionServiceMetrics implements SubscriptionServiceMetrics {
    /**
     * Type id of the counters registered by this class. Values below 1000 are reserved by Aeron;
     * 2000 is used by the cache tracing service and 2100 by the gateway metrics.
     */
    public static final int COUNTER_TYPE_ID = 2200;

    private static final String LABEL_PREFIX = "AeronCache-Subscriptions-";

    /**
     * Monotonic subscription events, each registered as its own media driver counter. The label is the
     * {@code <Event>} segment of the counter name and never contains a hyphen.
     */
    private enum Event {
        WHOLE_CACHE_SUBSCRIBE("WholeCacheSubscribe"),
        KEY_SUBSCRIBE("KeySubscribe"),
        DUPLICATE_SUBSCRIBE("DuplicateSubscribe"),
        UNSUBSCRIBE("Unsubscribe"),
        SESSION_CLOSED("SessionClosed"),
        UPDATE_PUBLISHED("UpdatePublished");

        private final String label;

        Event(String label) {
            this.label = label;
        }
    }

    private final Counter[] eventCounters;
    private final Counter activeSubscriptions;

    /**
     * @param aeron            client connected to the media driver the counters are registered with.
     * @param nodeId           identifier for this node, included in the counter labels.
     * @param subscriptionType the subscription service this instance measures (e.g. {@code cache},
     *                         {@code patch} or {@code counters}); must not contain a hyphen.
     */
    public AeronCounterSubscriptionServiceMetrics(Aeron aeron, String nodeId, String subscriptionType) {
        final String prefix = LABEL_PREFIX + subscriptionType + "-";
        final String suffix = "-node" + nodeId;
        eventCounters = new Counter[Event.values().length];
        for (Event event : Event.values()) {
            eventCounters[event.ordinal()] = aeron.addCounter(COUNTER_TYPE_ID, prefix + event.label + suffix);
        }
        activeSubscriptions = aeron.addCounter(COUNTER_TYPE_ID, prefix + "Active" + suffix);
        log.info("Registered {} Aeron subscription metrics counters for {} subscriptions on node {}",
                eventCounters.length + 1, subscriptionType, nodeId);
    }

    @Override
    public void onWholeCacheSubscription() {
        eventCounters[Event.WHOLE_CACHE_SUBSCRIBE.ordinal()].incrementOrdered();
    }

    @Override
    public void onKeySubscription() {
        eventCounters[Event.KEY_SUBSCRIBE.ordinal()].incrementOrdered();
    }

    @Override
    public void onDuplicateSubscription() {
        eventCounters[Event.DUPLICATE_SUBSCRIBE.ordinal()].incrementOrdered();
    }

    @Override
    public void onUnsubscription() {
        eventCounters[Event.UNSUBSCRIBE.ordinal()].incrementOrdered();
    }

    @Override
    public void onSessionClosed() {
        eventCounters[Event.SESSION_CLOSED.ordinal()].incrementOrdered();
    }

    @Override
    public void onUpdatePublished() {
        eventCounters[Event.UPDATE_PUBLISHED.ordinal()].incrementOrdered();
    }

    @Override
    public void setActiveSubscriptions(long count) {
        activeSubscriptions.setOrdered(count);
    }
}
