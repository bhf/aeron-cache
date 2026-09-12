package com.bhf.aeroncache.integration.streaming;

import com.bhf.aeroncache.integration.BackendTestResource;
import com.bhf.aeroncache.ws.bidi.messages.BidiClientMessage;
import com.bhf.aeroncache.ws.bidi.messages.WsOp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * A test client for the bidirectional websocket endpoint that issues commands and subscriptions over a
 * single persistent connection and records every frame the server sends back, queryable by
 * {@code correlationId}.
 *
 * <p>The websocket analogue of the gateway's {@code RecordingListener} test double: it exercises the
 * command-over-socket half of the BIDI protocol (which, unlike the subscribe/stream half, cannot be
 * covered by the existing HTTP-mutation abstract suites). Received frames are exposed as raw
 * {@link JsonNode} so tests assert on the JSON contract directly. Tests typically poll {@link #firstFrame}
 * with Awaitility.</p>
 */
public class BidiWsHelper implements AutoCloseable {

    private static final String BIDI_ENDPOINT = "/api/ws/v1/bidi";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<JsonNode> frames = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> connected = new CompletableFuture<>();

    private OkHttpClient client;
    private WebSocket webSocket;

    /**
     * Open the connection and block until the socket is established.
     *
     * @param backend the backend under test.
     * @return this helper.
     */
    public BidiWsHelper connect(BackendTestResource backend) {
        client = new OkHttpClient();
        var uri = backend.getBaseWsUri() + ":" + backend.getWsPort() + BIDI_ENDPOINT;
        var request = new Request.Builder().url(uri).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket ws, @NotNull Response response) {
                connected.complete(null);
            }

            @Override
            public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
                System.out.println("BIDI CLIENT RECV: " + text);
                try {
                    frames.add(OBJECT_MAPPER.readTree(text));
                } catch (Exception e) {
                    System.out.println("Could not parse BIDI frame: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket ws, @NotNull Throwable t, @Nullable Response response) {
                System.out.println("BIDI CLIENT FAILURE: " + t);
                if (!connected.isDone()) {
                    connected.completeExceptionally(t);
                }
            }
        });
        try {
            connected.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not connect BIDI websocket", e);
        }
        return this;
    }

    /**
     * Send a command frame.
     *
     * @param op            the operation.
     * @param correlationId the correlation id to echo.
     * @param cacheId       the target cache id.
     * @param key           the entry key, or {@code null}.
     * @param value         the entry value, or {@code null}.
     * @param ttl           the entry ttl in millis.
     * @param counterValue  the counter amount/value for counter operations.
     */
    public void command(WsOp op, String correlationId, String cacheId, String key, String value, long ttl, long counterValue) {
        ObjectNode frame = OBJECT_MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.COMMAND);
        frame.put("op", op.name());
        frame.put("correlationId", correlationId);
        if (cacheId != null) {
            frame.put("cacheId", cacheId);
        }
        if (key != null) {
            frame.put("key", key);
        }
        if (value != null) {
            frame.put("value", value);
        }
        frame.put("ttl", ttl);
        frame.put("counterValue", counterValue);
        send(frame.toString());
    }

    /**
     * Send a subscribe frame for a single whole cache.
     *
     * @param correlationId the correlation id to echo on streamed updates.
     * @param cacheId       the cache id.
     * @param counters      whether the counters flavour is targeted.
     * @param sendSnapshot  whether to request hydration.
     */
    public void subscribe(String correlationId, String cacheId, boolean counters, boolean sendSnapshot) {
        ObjectNode frame = OBJECT_MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.SUBSCRIBE);
        frame.put("correlationId", correlationId);
        frame.put("counters", counters);
        frame.put("sendSnapshot", sendSnapshot);
        ArrayNode caches = frame.putArray("caches");
        ObjectNode selector = caches.addObject();
        selector.put("cacheId", cacheId);
        selector.put("mode", "FULL");
        send(frame.toString());
    }

    /**
     * Send an unsubscribe frame.
     *
     * @param correlationId the correlation id to echo.
     * @param cacheId       the cache id.
     * @param counters      whether the counters flavour is targeted.
     */
    public void unsubscribe(String correlationId, String cacheId, boolean counters) {
        ObjectNode frame = OBJECT_MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.UNSUBSCRIBE);
        frame.put("correlationId", correlationId);
        frame.put("counters", counters);
        frame.put("cacheId", cacheId);
        send(frame.toString());
    }

    /**
     * Build a single bulk operation node for {@link #bulk}.
     *
     * @param operationType the {@code BulkOperationType} name (e.g. {@code "ADD_ITEM"}, {@code "INCREMENT_COUNTER"}).
     * @param requestId     the per-operation id, echoed on the matching response entry.
     * @param cacheId       the target cache id.
     * @param key           the entry key, or {@code null}.
     * @param value         the entry value, or {@code null}.
     * @param ttl           the entry ttl in millis.
     * @param counterValue  the counter amount/value for counter operations.
     * @return the operation node.
     */
    public ObjectNode bulkOp(String operationType, String requestId, String cacheId, String key, String value,
                             long ttl, long counterValue) {
        ObjectNode op = OBJECT_MAPPER.createObjectNode();
        op.put("operationType", operationType);
        op.put("requestId", requestId);
        op.put("ttl", ttl);
        op.put("counterValue", counterValue);
        if (cacheId != null) {
            op.put("cacheId", cacheId);
        }
        if (key != null) {
            op.put("key", key);
        }
        if (value != null) {
            op.put("value", value);
        }
        return op;
    }

    /**
     * Send a bulk operations frame batching the given operations, applied by the cluster in order.
     *
     * @param correlationId the correlation id to echo on the bulk response.
     * @param operations    the operations (see {@link #bulkOp}).
     */
    public void bulk(String correlationId, List<ObjectNode> operations) {
        ObjectNode frame = OBJECT_MAPPER.createObjectNode();
        frame.put("type", BidiClientMessage.BULK);
        frame.put("correlationId", correlationId);
        ArrayNode ops = frame.putArray("operations");
        operations.forEach(ops::add);
        send(frame.toString());
    }

    /**
     * Send a raw JSON frame verbatim (used to exercise malformed / unknown input).
     *
     * @param json the frame text.
     */
    public void send(String json) {
        webSocket.send(json);
    }

    /**
     * A fresh correlation id.
     *
     * @return a random correlation id.
     */
    public String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * A snapshot of all frames received so far.
     *
     * @return the received frames.
     */
    public List<JsonNode> frames() {
        return new ArrayList<>(frames);
    }

    /**
     * The frames received so far correlated to the given id.
     *
     * @param correlationId the correlation id.
     * @return the matching frames, in receive order.
     */
    public List<JsonNode> framesFor(String correlationId) {
        List<JsonNode> matching = new ArrayList<>();
        for (var frame : frames) {
            if (correlationId.equals(frame.path("correlationId").asText(null))) {
                matching.add(frame);
            }
        }
        return matching;
    }

    /**
     * The first frame of the given {@code type} correlated to the given id, if any.
     *
     * @param correlationId the correlation id.
     * @param type          the frame {@code type} discriminator.
     * @return the first matching frame, if present.
     */
    public Optional<JsonNode> firstFrame(String correlationId, String type) {
        for (var frame : frames) {
            if (correlationId.equals(frame.path("correlationId").asText(null))
                    && type.equals(frame.path("type").asText(null))) {
                return Optional.of(frame);
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Done");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}
