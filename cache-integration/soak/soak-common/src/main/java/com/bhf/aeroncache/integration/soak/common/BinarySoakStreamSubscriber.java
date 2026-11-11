package com.bhf.aeroncache.integration.soak.common;

import com.bhf.aeroncache.gateway.client.GatewayClient;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import io.aeron.Aeron;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

/**
 * A {@link SoakStreamSubscriber} over the binary Aeron transport: the subscription is observed on the same
 * {@link GatewayClient}/{@link SoakRecordingListener} the mutations flow over, rather than a separate URL
 * connection. {@link #poll()} drains the listener's shared stream-update queue (events are attributed by cache
 * id downstream, and each round uses freshly created unique caches), and because the subscription stays live
 * after an explicit unsubscribe this transport also exposes {@link LiveUnsubscribeProbe} for the
 * "silent after unsubscribe" negative check the URL transports cannot perform.
 */
final class BinarySoakStreamSubscriber implements SoakStreamSubscriber, LiveUnsubscribeProbe {

    private static final long PARK_NANOS = 50_000L;
    private static final AtomicLong SUB_CORR = new AtomicLong();

    private final GatewayClient client;
    private final SoakRecordingListener listener;
    private final String corr;

    BinarySoakStreamSubscriber(GatewayClient client, SoakRecordingListener listener, SubscriptionSpec spec) {
        this.client = client;
        this.listener = listener;
        // Clear any residual updates from the previous round before this round's subscription starts streaming.
        drain();
        this.corr = "sub-" + SUB_CORR.incrementAndGet();
        subscribe(spec);
    }

    private void subscribe(SubscriptionSpec spec) {
        if (!spec.keyScoped() && !spec.patch()) {
            // Whole-cache full mode: the key-less subscribe overload.
            sendWithRetry(() -> client.subscribe(corr, spec.cacheIds(), spec.hydrate(), spec.counters()));
        } else {
            var keys = new ArrayList<String>();
            for (int i = 0; i < spec.cacheIds().size(); i++) {
                keys.add(spec.keyScoped() ? spec.key() : null);
            }
            sendWithRetry(() -> client.subscribe(corr, spec.cacheIds(), keys, spec.patch(), spec.hydrate(), spec.counters()));
        }
    }

    @Override
    public void awaitAck(long timeoutNanos) {
        var deadline = System.nanoTime() + timeoutNanos;
        while (!listener.subscribeAcks.containsKey(corr)) {
            if (!client.isConnected()) {
                throw new IllegalStateException("gateway client disconnected awaiting subscribe ack " + corr);
            }
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("timed out awaiting subscribe ack " + corr);
            }
            LockSupport.parkNanos(PARK_NANOS);
        }
        var ack = listener.subscribeAcks.remove(corr);
        if (ack.status() != OperationStatus.SUCCESS) {
            throw new IllegalStateException("subscribe " + corr + " not acknowledged: " + ack.status());
        }
    }

    @Override
    public StreamEvent poll() {
        var u = listener.streamUpdates.poll();
        if (u == null) {
            return null;
        }
        return new StreamEvent(u.eventType().name(), u.cacheId(), u.key(), u.value());
    }

    @Override
    public boolean isHealthy() {
        return client.isConnected();
    }

    @Override
    public LiveUnsubscribeProbe liveUnsubscribeProbe() {
        return this;
    }

    @Override
    public void unsubscribe(List<String> cacheIds, boolean counters) {
        cacheIds.forEach(cache -> sendWithRetry(() -> client.unsubscribe("sub-" + SUB_CORR.incrementAndGet(), cache, counters)));
        drain();
    }

    @Override
    public void close() {
        // The still-alive caches are unsubscribed explicitly via the probe before close; here we only discard any
        // residual buffered updates so nothing carries into the next round.
        drain();
    }

    private void drain() {
        while (listener.streamUpdates.poll() != null) {
            // discard
        }
    }

    private void sendWithRetry(LongSupplier send) {
        while (send.getAsLong() == Aeron.NULL_VALUE) {
            LockSupport.parkNanos(PARK_NANOS);
        }
    }
}
