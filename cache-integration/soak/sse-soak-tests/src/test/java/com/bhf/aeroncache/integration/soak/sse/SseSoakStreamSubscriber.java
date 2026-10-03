package com.bhf.aeroncache.integration.soak.sse;

import com.bhf.aeroncache.http.responses.SubscriptionAck;
import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriber;
import com.bhf.aeroncache.integration.soak.common.StreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A soak-grade subscriber over the SSE streaming routes ({@code /api/sse/v1/...}). The subscription is encoded
 * in the connect URL; this client opens an {@link EventSource}, treats the SSE event named {@code "subscribed"}
 * as the readiness ack and buffers every subsequent {@code CacheUpdateEvent} data event into a bounded queue the
 * workload drains. Unlike the integration-test {@code SSEStreamingHelper} (collect-a-fixed-count-then-close), it
 * stays open for the whole round and never buffers unboundedly.
 */
final class SseSoakStreamSubscriber implements SoakStreamSubscriber {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Queue<StreamEvent> events = new ConcurrentLinkedQueue<>();
    private final CountDownLatch ack = new CountDownLatch(1);
    private volatile boolean failed;
    private volatile boolean closed;

    private final EventSource eventSource;

    SseSoakStreamSubscriber(EventSource.Factory factory, String uri) {
        var request = new Request.Builder().url(uri).build();
        this.eventSource = factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource source, String id, String type, String data) {
                handle(type, data);
            }

            @Override
            public void onFailure(EventSource source, Throwable t, Response response) {
                if (!closed) {
                    failed = true;
                }
            }
        });
    }

    private void handle(String type, String data) {
        // The ack arrives as an SSE event named "subscribed"; data events are named "message".
        if (SubscriptionAck.SUBSCRIBED.equals(type)) {
            ack.countDown();
            return;
        }
        final JsonNode node;
        try {
            node = MAPPER.readTree(data);
        } catch (Exception e) {
            failed = true;
            return;
        }
        var eventType = node.path("eventType").asText(null);
        if (eventType == null) {
            return;
        }
        events.add(new StreamEvent(eventType, textOrNull(node, "cacheId"),
                textOrNull(node, "itemKey"), valueText(node)));
    }

    @Override
    public void awaitAck(long timeoutNanos) {
        try {
            if (!ack.await(timeoutNanos, TimeUnit.NANOSECONDS)) {
                throw new IllegalStateException("timed out awaiting SSE subscription ack");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted awaiting SSE subscription ack", e);
        }
    }

    @Override
    public StreamEvent poll() {
        return events.poll();
    }

    @Override
    public boolean isHealthy() {
        return !failed;
    }

    @Override
    public void close() {
        closed = true;
        eventSource.cancel();
    }

    private static String textOrNull(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Renders itemValue as text whether the gateway sends it as a JSON scalar (counter number / string) or object. */
    private static String valueText(JsonNode node) {
        var value = node.get("itemValue");
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }
}
