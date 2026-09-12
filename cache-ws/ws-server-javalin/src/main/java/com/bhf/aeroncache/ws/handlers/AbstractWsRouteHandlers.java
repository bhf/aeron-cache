package com.bhf.aeroncache.ws.handlers;

import com.bhf.aeroncache.http.responses.SubscriptionAck;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.ws.application.CacheSubscriptionRequestPublisher;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import io.javalin.http.Context;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Shared websocket subscription wiring for a single cache flavour (regular caches or counters).
 *
 * <p>Concrete subclasses bind the handler to a specific {@link CacheSubscriptionRequestPublisher}, decide
 * whether patch mode is permitted for the flavour, and provide a label used in logging and error messages.</p>
 */
@Log4j2
public abstract class AbstractWsRouteHandlers {

    protected final CacheSubscriptionRequestPublisher subscriptionService;
    protected final boolean allowPatch;
    protected final String entityLabel;

    protected AbstractWsRouteHandlers(CacheSubscriptionRequestPublisher subscriptionService, boolean allowPatch, String entityLabel) {
        this.subscriptionService = subscriptionService;
        this.allowPatch = allowPatch;
        this.entityLabel = entityLabel;
    }

    /**
     * Setup websocket for subscriptions to a single cache.
     *
     * @param wsConfig The websocket config.
     */
    public void handleSingleCacheWs(WsConfig wsConfig) {
        configureWs(wsConfig, ctx -> onSingleCacheConnect(ctx, false));
    }

    /**
     * Setup websocket for subscriptions to a single cache with hydration.
     *
     * @param wsConfig The websocket config.
     */
    public void handleSingleCacheWsWithHydration(WsConfig wsConfig) {
        configureWs(wsConfig, ctx -> onSingleCacheConnect(ctx, true));
    }

    /**
     * Setup websocket for subscriptions to multiple caches.
     *
     * @param wsConfig The websocket config.
     */
    public void handleMultiCacheWs(WsConfig wsConfig) {
        configureWs(wsConfig, ctx -> onMultiCacheConnect(ctx, false));
    }

    /**
     * Setup websocket for subscriptions to multiple caches with hydration.
     *
     * @param wsConfig The websocket config.
     */
    public void handleMultiCacheWsWithHydration(WsConfig wsConfig) {
        configureWs(wsConfig, ctx -> onMultiCacheConnect(ctx, true));
    }

    private void configureWs(WsConfig wsConfig, Consumer<WsConnectContext> onConnect) {
        wsConfig.onConnect(onConnect::accept);
        wsConfig.onClose(this::onWsClose);
        wsConfig.onError(this::onWsError);
        wsConfig.onMessage(this::onWsMessage);
    }

    private void onSingleCacheConnect(WsConnectContext ctx, boolean hydrate) {
        try {
            ctx.enableAutomaticPings();
            var cacheId = ctx.pathParam("cacheId");
            log.info("Subscription request{} for {} cacheId: {} on ws sessionId: {}",
                    hydrate ? " with hydration" : "", entityLabel, cacheId, ctx.sessionId());
            subscribe(ctx, List.of(cacheId), hydrate);
        } catch (NumberFormatException e) {
            onParseError(ctx);
        }
    }

    private void onMultiCacheConnect(WsConnectContext ctx, boolean hydrate) {
        try {
            ctx.enableAutomaticPings();
            var cacheIds = ctx.pathParam("cacheIds");
            List<String> caches = Arrays.stream(cacheIds.split(",")).toList();
            log.info("Subscription request{} for {} cacheId: {} on ws sessionId: {}",
                    hydrate ? " with hydration" : "", entityLabel, caches, ctx.sessionId());
            subscribe(ctx, caches, hydrate);
        } catch (NumberFormatException e) {
            onParseError(ctx);
        }
    }

    private void subscribe(WsConnectContext ctx, List<String> caches, boolean hydrate) {
        var requestId = getRequestId(ctx.getUpgradeCtx$javalin());
        final Consumer<Void> subscriptionFailureHandler = _ ->
                ctx.closeSession(WsCloseStatus.SERVER_ERROR, "Couldn't subscribe to " + entityLabel);
        final Runnable subscriptionAckHandler = () -> ctx.send(SubscriptionAck.of(caches, requestId));
        var params = expandSubscription(ctx, caches, allowPatch);
        subscriptionService.subscribeToCache(WebsocketApplication.getCache(), subscriptionFailureHandler,
                subscriptionAckHandler, params.cacheIds(), params.keys(), params.mode(),
                ctx.sessionId(), requestId, hydrate, ctx::send);
    }

    private void onParseError(WsConnectContext ctx) {
        WebsocketApplication.statsTracker.getTotalErrors().incrementAndGet();
        log.warn("Couldn't parse cacheId correctly, path params: {}", ctx.pathParamMap());
        ctx.closeSession(WsCloseStatus.PROTOCOL_ERROR, "Couldn't parse cacheId");
    }

    private void onWsMessage(WsMessageContext ctx) {
        log.warn("Received message from websocket sessionId: {}, message: {}", ctx.sessionId(), ctx.message());
    }

    private void onWsError(WsErrorContext ctx) {
        log.warn("Got websocket error: {}", ctx);
        subscriptionService.handleWsError(WebsocketApplication.getCache(),
                getRequestId(ctx.getUpgradeCtx$javalin()), ctx.sessionId());
    }

    private void onWsClose(WsCloseContext ctx) {
        log.info("Websocket closed for sessionId: {}", ctx.sessionId());

        if (WebsocketApplication.isClusteredMode()) {
            subscriptionService.handleWsClosed(WebsocketApplication.getCache(),
                    getRequestId(ctx.getUpgradeCtx$javalin()), ctx.sessionId());
        }
    }

    /**
     * Parsed subscription request parameters expanded from the request URI query string.
     *
     * @param cacheIds The caches to subscribe to (parallel to {@code keys}).
     * @param keys     The keys parallel to {@code cacheIds}; a {@code null} entry denotes a whole-cache subscription.
     * @param mode     The subscription mode.
     */
    protected record SubscriptionParams(List<String> cacheIds, List<String> keys, SubscriptionMode mode) {
    }

    /**
     * Expand the optional {@code keys} and {@code mode} query parameters into parallel cacheId/key lists.
     *
     * <p>{@code ?mode=patch} selects patch mode (default is full). {@code ?keys=} is a comma-separated list of
     * tokens, where a token of the form {@code cacheId:key} targets a specific cache and a bare {@code key} token
     * applies to every cache in the route. When {@code keys} is absent the behaviour is a whole-cache subscription.</p>
     *
     * @param ctx        The websocket connect context.
     * @param baseCaches The caches identified from the request path.
     * @param allowPatch Whether patch mode is permitted for this route (patch is cache-only, not for counters).
     * @return The expanded subscription parameters.
     */
    protected static SubscriptionParams expandSubscription(WsConnectContext ctx, List<String> baseCaches, boolean allowPatch) {
        var modeParam = ctx.queryParam("mode");
        var mode = (allowPatch && "patch".equalsIgnoreCase(modeParam)) ? SubscriptionMode.PATCH : SubscriptionMode.FULL;

        var keysParam = ctx.queryParam("keys");
        if (keysParam == null || keysParam.isBlank()) {
            return new SubscriptionParams(baseCaches, null, mode);
        }

        Map<String, LinkedHashSet<String>> keysByCache = new LinkedHashMap<>();
        for (var token : keysParam.split(",")) {
            var trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            var sep = trimmed.indexOf(':');
            if (sep >= 0) {
                var cacheId = trimmed.substring(0, sep);
                var key = trimmed.substring(sep + 1);
                if (!key.isEmpty()) {
                    keysByCache.computeIfAbsent(cacheId, x -> new LinkedHashSet<>()).add(key);
                }
            } else {
                for (var cacheId : baseCaches) {
                    keysByCache.computeIfAbsent(cacheId, x -> new LinkedHashSet<>()).add(trimmed);
                }
            }
        }

        List<String> cacheIds = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (var cacheId : baseCaches) {
            var keySet = keysByCache.get(cacheId);
            if (keySet == null || keySet.isEmpty()) {
                cacheIds.add(cacheId);
                keys.add(null);
            } else {
                for (var key : keySet) {
                    cacheIds.add(cacheId);
                    keys.add(key);
                }
            }
        }
        return new SubscriptionParams(cacheIds, keys, mode);
    }

    /**
     * Build the requestId based on whether tracing is enabled.
     *
     * @param ctx The Context.
     * @return A requestId
     */
    protected static String getRequestId(Context ctx) {
        return WebsocketApplication.tracingServiceName != null && ctx != null ? getTraceBasedRequestId(ctx) : UUID.randomUUID().toString();
    }

    /**
     * Use the current span and trace Ids to build a requestId to
     * be sent to the Aeron Cache cluster.
     *
     * @param ctx The Context.
     * @return A requestId
     */
    static String getTraceBasedRequestId(Context ctx) {
        var currentSpanId = Span.current().getSpanContext().getSpanId();
        var currentTraceId = Span.current().getSpanContext().getTraceId();
        log.info("Creating requestId using traceID {} and spanID {}", currentTraceId, currentSpanId);
        return currentTraceId + "@" + currentSpanId;
    }
}
