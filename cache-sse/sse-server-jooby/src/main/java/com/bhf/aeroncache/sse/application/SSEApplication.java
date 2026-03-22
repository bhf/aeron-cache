package com.bhf.aeroncache.sse.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.http.responses.CacheUpdateEvent;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.ClusterClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.services.cluster.impl.RBClusterMessagePublisher;
import com.bhf.aeroncache.sse.config.SSEIdleStrategies;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.aeron.Aeron;
import io.aeron.RethrowingErrorHandler;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.jooby.*;
import io.jooby.exception.TypeMismatchException;
import io.jooby.handler.Cors;
import io.jooby.handler.CorsHandler;
import io.jooby.jackson.JacksonModule;
import io.jooby.netty.NettyServer;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Log4j2
public class SSEApplication extends Jooby {

    private static final int DEFAULT_SSE_PORT = 7072;
    private static final String API_PREFIX = "/api/sse/v1/cache/";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";
    private static final String MULTI_SUB_API_PREFIX = "/api/sse/v1/caches/";
    private static final boolean PRE_ENCODE_CACHE_REQUESTS = false;
    private static AeronCacheClusterListener client;
    private static CacheSubscriptionRequestPublisher subscriptionService;
    private static AeronCache cache;
    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);
    private static String tracingServiceName;
    private static AgentRunner agentRunner;
    private static AeronCluster aeronCluster;
    private static MediaDriver mediaDriver;
    private static final ObjectWriter writer = new ObjectMapper().writer();
    private static boolean CLUSTERED_MODE;

    public static void main(final String[] args) {
        startSSEInterface(args, DEFAULT_SSE_PORT);
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
        use(new CorsHandler(new Cors().setOrigin("http://localhost:3000")));
        sse(API_PREFIX + "{cacheId}", SSEApplication::handleSingleCacheSSE);
        sse(MULTI_SUB_API_PREFIX + "{cacheIds}", SSEApplication::handleMultiCacheSSE);
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

            subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, cacheId, serverSentEmitter.getId(),
                    requestId, consumer);
        } catch (TypeMismatchException e) {
            log.warn("Couldn't parse cacheId correctly, path params: {}", serverSentEmitter.getContext().pathMap());
        }
    }


    private static void handleMultiCacheSSE(ServerSentEmitter serverSentEmitter) {
        var cacheIds = serverSentEmitter.getContext().path("cacheIds").toString();
        log.info("Got cache Ids: "+cacheIds);
        String[] caches = cacheIds.split(",");
        for (var c : caches) {
            var requestId = UUID.randomUUID().toString();
            log.info("Subscription request for cacheId: {} on SSE sessionId: {}", c,
                    serverSentEmitter.getId());
            final Consumer<Void> subscriptionFailureHandler = _ ->
                    serverSentEmitter.close();

            final Consumer<CacheUpdateEvent> consumer = cacheUpdateEvent -> {
                serverSentEmitter.send("message", cacheUpdateEvent);
            };

            subscriptionService.subscribeToCache(cache, subscriptionFailureHandler, c, serverSentEmitter.getId(),
                    requestId, consumer);
        }
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

    private static void buildClusterConnection(String egressIP, String ingressEndpoints) {
        try {
            aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client, "HTTPClient",
                    mediaDriver);
            addClusterErrorHandler(aeronCluster);

            cache = new AeronCache() {
                @Override
                public void sendKeepAlive() {
                    aeronCluster.sendKeepAlive();
                }

                @Override
                public int pollEgress() {
                    return aeronCluster.pollEgress();
                }

                @Override
                public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int i) {
                    return aeronCluster.offer(msgBuffer, msgBufferOffset, i);
                }

                @Override
                public boolean isConnected() {
                    return !aeronCluster.isClosed();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Will try to reconnect");
            buildClusterConnection(egressIP, ingressEndpoints);
        }
    }

    public static void shutdown() {
        CloseHelper.close(aeronCluster);
        CloseHelper.close(mediaDriver);
        CloseHelper.close(agentRunner);
    }

    private static void addClusterErrorHandler(AeronCluster aeronCluster) {
        aeronCluster.context().errorHandler(throwable -> clusterConnected.set(false));
    }

    private static void setupCacheConnection() {
        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);
            System.out.println("Starting AeronCache Cluster Interface");

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();

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
            }

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider);
            client.setCacheResultsCallbacks(subscriptionService);

            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            System.out.println("CLUSTER_ADDRESSES=" + allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = List.of(allHosts.split(","));
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            System.out.println("Awaiting DNS Resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            System.out.println("DNS Resolution Complete. Building cluster connection now.");
            mediaDriver = ClusterUtils.launchEmbeddedMediaDriver();

            var cacheMode = System.getenv("CACHE_MODE");
            CLUSTERED_MODE = cacheMode == null || cacheMode.toUpperCase().equals("RAFT");

            System.out.println("Cache mode: " + cacheMode + ", using clustered mode: " + CLUSTERED_MODE);

            if (CLUSTERED_MODE) {
                buildClusterConnection(egressIP, ingressEndpoints);
            } else {
                final Aeron.Context aeronCtx = new Aeron.Context()
                        .aeronDirectoryName(mediaDriver.aeronDirectoryName());
                final Aeron aeron = Aeron.connect(aeronCtx);
                var requestPubHost = System.getenv("REQUEST_PUB_HOST");
                buildUnclusteredConnection(aeron, requestPubHost);
            }

            System.out.println("Building cluster agent for SSE service");
            var clusterClientAgentIdleStrategy = SSEIdleStrategies.clusterClientAgentIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = SSEIdleStrategies.clusterMessagePublisherIdleStrategy.get();
            var agent = PRE_ENCODE_CACHE_REQUESTS ?
                    new ClusterClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent") :
                    new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy, new ClusterMessagePublisher(cache,
                            clusterMessagePublisherIdleStrategy, cacheRequestEncoder), "AeronCache-CacheClient-Agent");

            var errorHandler = aeronCluster != null ? ClusterUtils.getAgentRunnerErrorHandler(aeronCluster) :
                    new RethrowingErrorHandler();
            var errorCounter = aeronCluster != null ? ClusterUtils.getAgentErrorCounter(aeronCluster, "SSEClient") :
                    null;
            final IdleStrategy agentRunnerIdleStrategy = SSEIdleStrategies.agentRunnerIdleStrategy.get();
            agentRunner = new AgentRunner(agentRunnerIdleStrategy, errorHandler, errorCounter, agent);
            clusterConnected.set(true);
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
        return STR."\{currentTraceId}@\{currentSpanId}";
    }

}
