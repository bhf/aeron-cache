package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.http.responses.SubscriptionAck;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCountersRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.sse.config.SSEIdleStrategies;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.services.ReconnectingAeronCache;
import com.bhf.aeroncache.utils.RingBufferUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.aeron.Aeron;
import io.aeron.RethrowingErrorHandler;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.jooby.*;
import io.jooby.exception.TypeMismatchException;
import io.jooby.handler.Cors;
import io.jooby.handler.CorsHandler;
import io.jooby.jackson.JacksonModule;
import io.jooby.netty.NettyServer;
import io.opentelemetry.api.trace.Span;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class SSEApplication extends Jooby {

    @Setter
    private static int DEFAULT_SSE_PORT = 7072;
    private static final String CACHE_API_PREFIX = "/api/sse/v1/cache/";
    private static final String CACHE_MULTI_SUB_API_PREFIX = "/api/sse/v1/caches/";
    private static final String COUNTERS_API_PREFIX = "/api/sse/v1/counter/";
    private static final String COUNTERS_MULTI_SUB_API_PREFIX = "/api/sse/v1/counters/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;
    private static AeronCacheClusterListener client;
    private static CacheSubscriptionRequestPublisher subscriptionService;
    private static CacheSubscriptionRequestPublisher countersSubscriptionService;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static String tracingServiceName;
    private static AgentRunner agentRunner;
    private static MediaDriver mediaDriver;
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static boolean CLUSTERED_MODE;
    public static int BOUND_PORT;

    public static void main(final String[] args) {
        BOUND_PORT = startSSEInterface(args, DEFAULT_SSE_PORT);
    }

    public static int startSSEInterface(String[] args, int port) {
        System.out.println("Starting SSE interface");
        tracingServiceName = System.getenv("OTEL_SERVICE_NAME");

        var boundPort = startHTTPServer(args, port);
        setupCacheConnection();
        return boundPort;
    }

    {
        install(new JacksonModule());
        use(new CorsHandler(new Cors().setOrigin("http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:3003",
                "http://localhost:3004",
                "http://localhost:3005")));
        sse(CACHE_API_PREFIX + "hydrate/{cacheId}", SSEApplication::handleSingleCacheSSEWithHydration);
        sse(CACHE_MULTI_SUB_API_PREFIX + "hydrate/{cacheIds}", SSEApplication::handleMultiCacheSSEWithHydration);
        sse(CACHE_API_PREFIX + "{cacheId}", SSEApplication::handleSingleCacheSSE);
        sse(CACHE_MULTI_SUB_API_PREFIX + "{cacheIds}", SSEApplication::handleMultiCacheSSE);

        sse(COUNTERS_API_PREFIX + "hydrate/{cacheId}", SSEApplication::handleSingleCountersCacheSSEWithHydration);
        sse(COUNTERS_MULTI_SUB_API_PREFIX + "hydrate/{cacheIds}", SSEApplication::handleMultiCountersCacheSSEWithHydration);
        sse(COUNTERS_API_PREFIX + "{cacheId}", SSEApplication::handleSingleCountersCacheSSE);
        sse(COUNTERS_MULTI_SUB_API_PREFIX + "{cacheIds}", SSEApplication::handleMultiCountersCacheSSE);

        get(LIVENESS, SSEApplication::handleGetLiveness);
        get(READINESS, SSEApplication::handleGetReadiness);
    }

    private static Object handleGetReadiness(Context ctx) {
        if (clusterConnected.get()) {
            return ctx.send(StatusCode.OK);
        } else {
            return ctx.send(StatusCode.SERVICE_UNAVAILABLE);
        }
    }

    private static Object handleGetLiveness(Context ctx) {
        if (clusterConnected.get()) {
            return ctx.send(StatusCode.OK);
        } else {
            return ctx.send(StatusCode.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Parsed subscription request parameters expanded from the request URI query string.
     *
     * @param cacheIds The caches to subscribe to (parallel to {@code keys}).
     * @param keys     The keys parallel to {@code cacheIds}; a {@code null} entry denotes a whole-cache subscription.
     * @param mode     The subscription mode.
     */
    private record SubscriptionParams(List<String> cacheIds, List<String> keys, SubscriptionMode mode) {
    }

    /**
     * Expand the optional {@code keys} and {@code mode} query parameters into parallel cacheId/key lists.
     *
     * <p>{@code ?mode=patch} selects patch mode (default is full). {@code ?keys=} is a comma-separated list of
     * tokens, where a token of the form {@code cacheId:key} targets a specific cache and a bare {@code key} token
     * applies to every cache in the route. When {@code keys} is absent the behaviour is a whole-cache subscription.</p>
     *
     * @param ctx        The request context.
     * @param baseCaches The caches identified from the request path.
     * @param allowPatch Whether patch mode is permitted for this route (patch is cache-only, not for counters).
     * @return The expanded subscription parameters.
     */
    private static SubscriptionParams expandSubscription(Context ctx, List<String> baseCaches, boolean allowPatch) {
        var modeParam = ctx.query("mode").valueOrNull();
        var mode = (allowPatch && "patch".equalsIgnoreCase(modeParam)) ? SubscriptionMode.PATCH : SubscriptionMode.FULL;

        var keysParam = ctx.query("keys").valueOrNull();
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
     * Build a handler that emits a subscription-confirmed ack (a {@code subscribed} SSE event) once the
     * cluster has registered the subscription, so clients know updates will now be delivered.
     *
     * @param emitter   The SSE emitter for the session.
     * @param caches    The caches the subscription covers.
     * @param requestId The request ID.
     * @return the ack handler.
     */
    private static Runnable subscriptionAck(ServerSentEmitter emitter, List<String> caches, String requestId) {
        return () -> {
            try {
                emitter.send(SubscriptionAck.SUBSCRIBED, writer.writeValueAsString(SubscriptionAck.of(caches, requestId)));
            } catch (JsonProcessingException e) {
                log.error("Error trying to convert subscription ack to JSON", e);
            }
        };
    }

    private static void handleSingleCacheSSE(ServerSentEmitter serverSentEmitter) {
        try {
            var cacheId = serverSentEmitter.getContext().path("cacheId").toString();
            var requestId = getRequestId(serverSentEmitter.getContext());
            log.info("Subscription request for cacheId: {} on SSE sessionId: {}", cacheId, serverSentEmitter.getId());

            serverSentEmitter.onClose(() -> {
                log.warn("Closed on " + serverSentEmitter.getId());
                if (CLUSTERED_MODE) {
                    subscriptionService.handleSSEClosed(cache, requestId, serverSentEmitter.getId());
                }
            });

            serverSentEmitter.keepAlive(60, TimeUnit.DAYS);

            final Consumer<Void> subscriptionFailureHandler = _ ->
                    serverSentEmitter.close();

            final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
                try {
                    final var res = writer.writeValueAsString(cacheUpdateEvent);
                    serverSentEmitter.send("message", res);
                } catch (JsonProcessingException e) {
                    log.error("Error trying to convert cache update event to JSON", e);
                }
            };

            var params = expandSubscription(serverSentEmitter.getContext(), List.of(cacheId), true);
            subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                    serverSentEmitter.getId(), requestId, false, consumer);
        } catch (TypeMismatchException e) {
            log.warn("Couldn't parse cacheId correctly, path params: {}", serverSentEmitter.getContext().pathMap());
        }
    }

    private static void handleSingleCountersCacheSSE(ServerSentEmitter serverSentEmitter) {
        try {
            var cacheId = serverSentEmitter.getContext().path("cacheId").toString();
            var requestId = getRequestId(serverSentEmitter.getContext());
            log.info("Subscription request for counters cacheId: {} on SSE sessionId: {}", cacheId, serverSentEmitter.getId());

            serverSentEmitter.onClose(() -> {
                log.warn("Closed on " + serverSentEmitter.getId());
                if (CLUSTERED_MODE) {
                    subscriptionService.handleSSEClosed(cache, requestId, serverSentEmitter.getId());
                }
            });

            serverSentEmitter.keepAlive(60, TimeUnit.DAYS);

            final Consumer<Void> subscriptionFailureHandler = _ ->
                    serverSentEmitter.close();

            final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
                try {
                    final var res = writer.writeValueAsString(cacheUpdateEvent);
                    serverSentEmitter.send("message", res);
                } catch (JsonProcessingException e) {
                    log.error("Error trying to convert counters cache update event to JSON", e);
                }
            };

            var params = expandSubscription(serverSentEmitter.getContext(), List.of(cacheId), false);
            countersSubscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                    serverSentEmitter.getId(), requestId, false, consumer);
        } catch (TypeMismatchException e) {
            log.warn("Couldn't parse cacheId correctly, path params: {}", serverSentEmitter.getContext().pathMap());
        }
    }

    private static void handleSingleCacheSSEWithHydration(ServerSentEmitter serverSentEmitter) {
        try {
            var cacheId = serverSentEmitter.getContext().path("cacheId").toString();
            var requestId = getRequestId(serverSentEmitter.getContext());
            log.info("Subscription request for cacheId: {} on SSE sessionId: {}", cacheId, serverSentEmitter.getId());

            serverSentEmitter.onClose(() -> {
                log.warn("Closed on " + serverSentEmitter.getId());
                if (CLUSTERED_MODE) {
                    subscriptionService.handleSSEClosed(cache, requestId, serverSentEmitter.getId());
                }
            });

            serverSentEmitter.keepAlive(60, TimeUnit.DAYS);

            final Consumer<Void> subscriptionFailureHandler = _ ->
                    serverSentEmitter.close();

            final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
                try {
                    final var res = writer.writeValueAsString(cacheUpdateEvent);
                    serverSentEmitter.send("message", res);
                } catch (JsonProcessingException e) {
                    log.error("Error trying to convert cache update event to JSON", e);
                }
            };

            var params = expandSubscription(serverSentEmitter.getContext(), List.of(cacheId), true);
            subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                    serverSentEmitter.getId(), requestId, true, consumer);
        } catch (TypeMismatchException e) {
            log.warn("Couldn't parse cacheId correctly, path params: {}", serverSentEmitter.getContext().pathMap());
        }
    }

    private static void handleSingleCountersCacheSSEWithHydration(ServerSentEmitter serverSentEmitter) {
        try {
            var cacheId = serverSentEmitter.getContext().path("cacheId").toString();
            var requestId = getRequestId(serverSentEmitter.getContext());
            log.info("Subscription request for counters cacheId: {} on SSE sessionId: {}", cacheId, serverSentEmitter.getId());

            serverSentEmitter.onClose(() -> {
                log.warn("Closed on " + serverSentEmitter.getId());
                if (CLUSTERED_MODE) {
                    countersSubscriptionService.handleSSEClosed(cache, requestId, serverSentEmitter.getId());
                }
            });

            serverSentEmitter.keepAlive(60, TimeUnit.DAYS);

            final Consumer<Void> subscriptionFailureHandler = _ ->
                    serverSentEmitter.close();

            final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
                try {
                    final var res = writer.writeValueAsString(cacheUpdateEvent);
                    serverSentEmitter.send("message", res);
                } catch (JsonProcessingException e) {
                    log.error("Error trying to convert counters cache update event to JSON", e);
                }
            };

            var params = expandSubscription(serverSentEmitter.getContext(), List.of(cacheId), false);
            countersSubscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                    serverSentEmitter.getId(), requestId, true, consumer);
        } catch (TypeMismatchException e) {
            log.warn("Couldn't parse cacheId correctly, path params: {}", serverSentEmitter.getContext().pathMap());
        }
    }


    private static void handleMultiCacheSSE(ServerSentEmitter serverSentEmitter) {
        var cacheIds = serverSentEmitter.getContext().path("cacheIds").toString();
        log.info("Got cache Ids: "+cacheIds);
        List<String> caches = Arrays.stream(cacheIds.split(",")).toList();

        var requestId = UUID.randomUUID().toString();
        log.info("Subscription request for cacheId: {} on SSE sessionId: {}", caches,
                serverSentEmitter.getId());
        final Consumer<Void> subscriptionFailureHandler = _ ->
                serverSentEmitter.close();

        final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
            try {
                final var res = writer.writeValueAsString(cacheUpdateEvent);
                serverSentEmitter.send("message", res);
            } catch (JsonProcessingException e) {
                log.error("Error trying to convert cache update event to JSON", e);
            }
        };

        var params = expandSubscription(serverSentEmitter.getContext(), caches, true);
        subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                serverSentEmitter.getId(), requestId, false, consumer);
    }

    private static void handleMultiCountersCacheSSE(ServerSentEmitter serverSentEmitter) {
        var cacheIds = serverSentEmitter.getContext().path("cacheIds").toString();
        log.info("Got cache Ids: "+cacheIds);
        List<String> caches = Arrays.stream(cacheIds.split(",")).toList();

        var requestId = UUID.randomUUID().toString();
        log.info("Subscription request for counters cacheId: {} on SSE sessionId: {}", caches,
                serverSentEmitter.getId());
        final Consumer<Void> subscriptionFailureHandler = _ ->
                serverSentEmitter.close();

        final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
            try {
                final var res = writer.writeValueAsString(cacheUpdateEvent);
                serverSentEmitter.send("message", res);
            } catch (JsonProcessingException e) {
                log.error("Error trying to convert counters cache update event to JSON", e);
            }
        };

        var params = expandSubscription(serverSentEmitter.getContext(), caches, false);
        countersSubscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                serverSentEmitter.getId(), requestId, false, consumer);
    }

    private static void handleMultiCacheSSEWithHydration(ServerSentEmitter serverSentEmitter) {
        var cacheIds = serverSentEmitter.getContext().path("cacheIds").toString();
        log.info("Got cache Ids: "+cacheIds);
        List<String> caches = Arrays.stream(cacheIds.split(",")).toList();

        var requestId = UUID.randomUUID().toString();
        log.info("Subscription request for cacheId: {} on SSE sessionId: {}", caches,
                serverSentEmitter.getId());
        final Consumer<Void> subscriptionFailureHandler = _ ->
                serverSentEmitter.close();

        final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
            try {
                final var res = writer.writeValueAsString(cacheUpdateEvent);
                serverSentEmitter.send("message", res);
            } catch (JsonProcessingException e) {
                log.error("Error trying to convert cache update event to JSON", e);
            }
        };

        var params = expandSubscription(serverSentEmitter.getContext(), caches, true);
        subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                serverSentEmitter.getId(), requestId, true, consumer);
    }

    private static void handleMultiCountersCacheSSEWithHydration(ServerSentEmitter serverSentEmitter) {
        var cacheIds = serverSentEmitter.getContext().path("cacheIds").toString();
        log.info("Got cache Ids: "+cacheIds);
        List<String> caches = Arrays.stream(cacheIds.split(",")).toList();

        var requestId = UUID.randomUUID().toString();
        log.info("Subscription request for counters cacheId: {} on SSE sessionId: {}", caches,
                serverSentEmitter.getId());
        final Consumer<Void> subscriptionFailureHandler = _ ->
                serverSentEmitter.close();

        final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
            try {
                final var res = writer.writeValueAsString(cacheUpdateEvent);
                serverSentEmitter.send("message", res);
            } catch (JsonProcessingException e) {
                log.error("Error trying to convert counters cache update event to JSON", e);
            }
        };

        var params = expandSubscription(serverSentEmitter.getContext(), caches, false);
        countersSubscriptionService.subscribeToCache(cache, subscriptionFailureHandler, subscriptionAck(serverSentEmitter, params.cacheIds(), requestId), params.cacheIds(), params.keys(), params.mode(),
                serverSentEmitter.getId(), requestId, true, consumer);
    }

    private static void buildUnclusteredConnection(Aeron aeron, String requestPubHost) {

        var requestPublicationChannel = "aeron:udp?endpoint=" + requestPubHost + ":6008|alias=AC-unclustered-requests";
        int requestPublicationStream = 1;
        var requestPublication = aeron.addPublication(requestPublicationChannel,
                requestPublicationStream);

        var hostname = DNSUtils.getThisHostName();
        String responseSubscriptionChannel = "aeron:udp?endpoint=" + hostname + ":6007|alias=AC-unclustered-responses";
        int responseSubscriptionStream = 2;
        var responseSubscription = aeron.addSubscription(responseSubscriptionChannel,
                responseSubscriptionStream);

        cache = new AeronCache() {
            @Override
            public void sendKeepAlive() {
            }

            @Override
            public int pollEgress() {
                return 0;
            }

            @Override
            public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int i) {
                long res = 0;
                while ((res = requestPublication.offer(msgBuffer, msgBufferOffset, i)) < 0) {
                    aeron.context().idleStrategy().idle();
                }
                return res;
            }

            @Override
            public boolean isConnected() {
                return true;
            }
        };

        AeronCacheClusterListener egressListener = client;
        FragmentHandler egressFragmentHandler = (buffer, offset, length, header)
                -> egressListener.onMessage(header.sessionId(),
                System.currentTimeMillis(),
                buffer, offset, length, header);

        Agent serverAgent = new Agent() {
            @Override
            public int doWork() throws Exception {
                return responseSubscription.poll(egressFragmentHandler, Integer.MAX_VALUE);
            }

            @Override
            public String roleName() {
                return "AC-Unclustered-requests-listener";
            }
        };

        IdleStrategy unclusteredAgentIdleStrategy = SSEIdleStrategies.unclusteredIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(unclusteredAgentIdleStrategy,
                Throwable::printStackTrace,
                null, serverAgent);

        AgentRunner.startOnThread(serverAgentRunner);
        clusterConnected.set(true);
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        ReconnectingAeronCache reconnectingCache = new ReconnectingAeronCache(egressIP, ingressEndpoints, client, "SSEClient",
                aeronDirectory, clusterConnected::set);
        reconnectingCache.connect();
        cache = reconnectingCache;
    }

    private static void setupCacheConnection() {
        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();
            var countersResponseDecoder = clientFactory.getCountersResponseDecoder();
            var countersRequestEncoder = clientFactory.getCountersRequestEncoder();

            if (PRE_ENCODE_CACHE_REQUESTS) {
                // We encode the SBE messages before dropping them onto an Agrona RB for
                // sending directly to the cluster
                var requestPublisher = new RBClusterMessagePublisher(cache, rb,
                        SSEIdleStrategies.clusterMessagePublisherIdleStrategy.get(), cacheRequestEncoder);
                subscriptionService = new CacheSubscriptionRequestPublisher(requestPublisher);
            } else {
                // Drop normalised cache requests onto an Agrona RB for encoding
                // to SBE on the Agent thread
                CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
                subscriptionService = new CacheSubscriptionRequestPublisher(rbPublisher);

                CacheRequestPublisher<String, String, Long> countersRbPublisher = new RBCountersRequestPublisher(rb);
                countersSubscriptionService = new CacheSubscriptionRequestPublisher(countersRbPublisher);
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(subscriptionService);
            client.setCountersResultsCallbacks(countersSubscriptionService);
            client.setCountersCacheResponseDecoder(countersResponseDecoder);

            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = allHosts!=null ? List.of(allHosts.split(",")) : List.of("localhost");
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            var launchEmbeddedStr = System.getenv("LAUNCH_EMBEDDED");
            boolean launchEmbedded = launchEmbeddedStr == null || Boolean.parseBoolean(launchEmbeddedStr);
            if (launchEmbedded) {
                mediaDriver = ClusterUtils.launchEmbeddedMediaDriver();
            }

            final Aeron.Context aeronCtx = new Aeron.Context();
            if (launchEmbedded) {
                aeronCtx.aeronDirectoryName(mediaDriver.aeronDirectoryName());
            } else {
                String externalAeronDir = System.getenv("AERON_DIR");
                if (externalAeronDir != null) {
                    aeronCtx.aeronDirectoryName(externalAeronDir);
                }
            }

            var cacheMode = System.getenv("CACHE_MODE");
            CLUSTERED_MODE = cacheMode == null || cacheMode.toUpperCase().equals("RAFT");

            System.out.println("Cache mode: " + cacheMode + ", using clustered mode: " + CLUSTERED_MODE);

            if (CLUSTERED_MODE) {
                buildClusterConnection(egressIP, ingressEndpoints, aeronCtx.aeronDirectoryName());
            } else {
                final Aeron aeron = Aeron.connect(aeronCtx);
                var requestPubHost = System.getenv("REQUEST_PUB_HOST");
                buildUnclusteredConnection(aeron, requestPubHost);
            }

            System.out.println("Building cluster agent for SSE service");
            var clusterClientAgentIdleStrategy = SSEIdleStrategies.clusterClientAgentIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = SSEIdleStrategies.clusterMessagePublisherIdleStrategy.get();

            var countersProtocolPublisher = new ClusterMessagePublisher<>(cache, clusterMessagePublisherIdleStrategy, countersRequestEncoder);

            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), countersProtocolPublisher, "AeronCache-CacheClient-Agent");

            var errorHandler = new RethrowingErrorHandler();
            var errorCounter = (org.agrona.concurrent.status.AtomicCounter) null;
            final IdleStrategy agentRunnerIdleStrategy = SSEIdleStrategies.agentRunnerIdleStrategy.get();
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            AgentRunner.startOnThread(agentRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CacheClientFactory getCacheClientFactory() {
        ServiceLoader<CacheClientFactory> service = ServiceLoader.load(CacheClientFactory.class);
        Optional<CacheClientFactory> first = service.findFirst();

        if (first.isPresent()) {
            return first.get();
        } else {
            throw new IllegalStateException("No CacheClientFactory found.");
        }
    }

    private static int startHTTPServer(String[] args, int port) {
        var serverOptions = new ServerOptions().setPort(port);
        var nettyServer = new NettyServer(serverOptions);
        runApp(args, nettyServer, SSEApplication::new);
        log.info("Launched SSE interface on port {}",serverOptions.getPort());
        return serverOptions.getPort();
    }

    private static String getRequestId(Context ctx) {
        return tracingServiceName != null && ctx != null ? getTraceBasedRequestId(ctx) : UUID.randomUUID().toString();
    }

    /**
     * Use the current span and trace Ids to build a requestId to
     * be sent to the Aeron Cache cluster.
     *
     * @param ctx
     * @return
     */
    private static String getTraceBasedRequestId(Context ctx) {
        var currentSpanId = Span.current().getSpanContext().getSpanId();
        var currentTraceId = Span.current().getSpanContext().getTraceId();
        log.info("Creating requestId using traceID {} and spanID {}", currentTraceId, currentSpanId);
        return currentTraceId + "@" + currentSpanId;
    }

}
