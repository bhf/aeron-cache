package com.bhf.aeroncache.integration.soak.streamingws;

import com.bhf.aeroncache.http.responses.SubscriptionAck;
import com.bhf.aeroncache.integration.soak.common.SoakStreamSubscriber;
import com.bhf.aeroncache.integration.soak.common.StreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A soak-grade subscriber over the one-directional streaming WebSocket routes ({@code /api/ws/v1/...}). The
 * subscription is encoded entirely in the connect URL; this client opens the socket, treats the
 * {@code {"type":"subscribed"}} frame as the readiness ack and buffers every subsequent {@code CacheUpdateEvent}
 * frame into a bounded queue the workload drains. Unlike the integration-test {@code WSStreamingHelper} (which
 * collects a fixed count then closes), it stays open for the whole round and its buffer never grows unbounded.
 */
final class WsSoakStreamSubscriber implements SoakStreamSubscriber {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Queue<StreamEvent> events = new ConcurrentLinkedQueue<>();
    private final CountDownLatch ack = new CountDownLatch(1);
    private volatile boolean failed;
    private volatile boolean closed;

    private final WebSocket webSocket;

    WsSoakStreamSubscriber(OkHttpClient client, String uri) {
        var request = new Request.Builder().url(uri).build();
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket ws, String text) {
                handle(text);
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                if (!closed) {
                    failed = true;
                }
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                if (!closed && code != 1000) {
                    failed = true;
                }
            }
        });
    }

    private void handle(String text) {
        final JsonNode node;
        try {
            node = MAPPER.readTree(text);
        } catch (Exception e) {
            failed = true;
            return;
        }
        // The ack carries a `type` field ("subscribed"); data frames carry `eventType` instead.
        if (SubscriptionAck.SUBSCRIBED.equals(node.path("type").asText(null))) {
            ack.countDown();
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
                throw new IllegalStateException("timed out awaiting WS subscription ack");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted awaiting WS subscription ack", e);
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
        webSocket.close(1000, "round complete");
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
