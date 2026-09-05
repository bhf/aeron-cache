package com.bhf.aeroncache.gateway.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.gateway.codec.GatewayResponseWriter;
import com.bhf.aeroncache.gateway.config.GatewayIdleStrategies;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.ReconnectingAeronCache;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCountersRequestPublisher;
import com.bhf.aeroncache.services.cacheclient.CacheClientFactory;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import io.aeron.Aeron;
import io.aeron.RethrowingErrorHandler;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.javalin.Javalin;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point for the Aeron gateway.
 * <p>
 * The gateway exposes the full cache/counter command surface (as offered by the HTTP
 * server) plus streaming subscriptions (as offered by the websocket/SSE servers) over
 * Aeron transport. Clients connect over an Aeron request channel and receive responses
 * and streaming updates over Aeron response channels ({@code control-mode=response}),
 * giving a single low latency, bidirectional endpoint.
 * <p>
 * Two agents run:
 * <ul>
 *     <li>{@link CacheClientAgent} - drains the request ring buffer and talks to the cluster.</li>
 *     <li>{@link GatewayIngressAgent} - the client facing Aeron endpoint.</li>
 * </ul>
 * A small HTTP server exposes liveness/readiness/prometheus for orchestration.
 */
@Log4j2
public class GatewayApplication {

    public static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    private static final String LIVENESS = "/liveness/";
    private static final String READINESS = "/readiness/";

    private static final int DEFAULT_HTTP_PORT = 7073;
    private static final int DEFAULT_REQUEST_PORT = 7075;
    private static final int DEFAULT_RESPONSE_CONTROL_PORT = 7076;
    private static final int REQUEST_STREAM_ID = 100;
    private static final int RESPONSE_STREAM_ID = 101;

    private static final AtomicBoolean clusterConnected = new AtomicBoolean(false);

    private static AeronCache cache;
    private static AeronCacheClusterListener client;
    private static GatewaySubscriptionPublisher cacheSubs;
    private static GatewaySubscriptionPublisher countersSubs;
    private static MediaDriver mediaDriver;
    private static Aeron gatewayAeron;
    private static AgentRunner clusterAgentRunner;
    private static AgentRunner ingressAgentRunner;
    private static boolean clusteredMode;

    public static int BOUND_PORT;

    public static void main(String[] args) {
        BOUND_PORT = start(DEFAULT_HTTP_PORT);
    }

    public static int start(int httpPort) {
        log.info("Starting Aeron gateway");

        var app = startHTTPServer(httpPort);

        try {
            ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(4096);

            CacheClientFactory clientFactory = getCacheClientFactory();
            var cacheRequestEncoder = clientFactory.getCacheRequestEncoder();
            var responseDecoder = clientFactory.getCacheResponseDecoder();
            var schemaDetailsProvider = clientFactory.getSchemaDetails();
            var indexSupplier = clientFactory.getIndexSupplier();
            var keySupplier = clientFactory.getKeySupplier();
            var valueSupplier = clientFactory.getValueSupplier();
            var countersResponseDecoder = clientFactory.getCountersResponseDecoder();
            var countersRequestEncoder = clientFactory.getCountersRequestEncoder();

            CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
            cacheSubs = new GatewaySubscriptionPublisher(rbPublisher);

            CacheRequestPublisher<String, String, Long> countersRbPublisher = new RBCountersRequestPublisher(rb);
            countersSubs = new GatewaySubscriptionPublisher(countersRbPublisher);

            client = new AeronCacheClusterListener(responseDecoder, schemaDetailsProvider, indexSupplier, keySupplier, valueSupplier);
            client.setCacheResultsCallbacks(cacheSubs);
            client.setCountersResultsCallbacks(countersSubs);
            client.setCountersCacheResponseDecoder(countersResponseDecoder);

            var allHosts = System.getenv("CLUSTER_ADDRESSES");
            log.info("CLUSTER_ADDRESSES={}", allHosts);

            var egressIP = DNSUtils.getThisHostName();
            var hostArray = allHosts != null ? List.of(allHosts.split(",")) : List.of("localhost");
            var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

            log.info("Awaiting DNS resolution");
            for (int i = 0; i < hostArray.size(); i++) {
                DNSUtils.awaitDnsResolution(hostArray, i);
            }

            var launchEmbeddedStr = System.getenv("LAUNCH_EMBEDDED");
            boolean launchEmbedded = launchEmbeddedStr == null || Boolean.parseBoolean(launchEmbeddedStr);
            if (launchEmbedded) {
                mediaDriver = ClusterUtils.launchEmbeddedMediaDriver();
            }

            final Aeron.Context aeronCtx = new Aeron.Context();
            final String aeronDir;
            if (launchEmbedded) {
                aeronDir = mediaDriver.aeronDirectoryName();
            } else {
                aeronDir = System.getenv("AERON_DIR");
            }
            if (aeronDir != null) {
                aeronCtx.aeronDirectoryName(aeronDir);
            }

            var cacheMode = System.getenv("CACHE_MODE");
            clusteredMode = cacheMode == null || cacheMode.equalsIgnoreCase("RAFT");
            log.info("Cache mode: {}, clustered: {}", cacheMode, clusteredMode);

            if (clusteredMode) {
                buildClusterConnection(egressIP, ingressEndpoints, aeronCtx.aeronDirectoryName());
            } else {
                final Aeron aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronCtx.aeronDirectoryName()));
                buildUnclusteredConnection(aeron, System.getenv("REQUEST_PUB_HOST"));
            }

            var clusterClientAgentIdleStrategy = clusteredMode ? GatewayIdleStrategies.clusterClientAgentIdleStrategy.get() : GatewayIdleStrategies.unclusteredIdleStrategy.get();
            var clusterMessagePublisherIdleStrategy = clusteredMode ? GatewayIdleStrategies.clusterMessagePublisherIdleStrategy.get() : GatewayIdleStrategies.unclusteredIdleStrategy.get();
            var agentRunnerIdleStrategy = clusteredMode ? GatewayIdleStrategies.agentRunnerIdleStrategy.get() : GatewayIdleStrategies.unclusteredIdleStrategy.get();

            var countersProtocolPublisher = new ClusterMessagePublisher<>(cache, clusterMessagePublisherIdleStrategy, countersRequestEncoder);
            var agent = new CacheClientAgent(cache, rb, clusterClientAgentIdleStrategy,
                    new ClusterMessagePublisher(cache, clusterMessagePublisherIdleStrategy, cacheRequestEncoder),
                    countersProtocolPublisher, "AeronCache-CacheClient-Agent");

            clusterAgentRunner = new AgentRunner(agentRunnerIdleStrategy, new RethrowingErrorHandler(), null, agent);
            AgentRunner.startOnThread(clusterAgentRunner);

            startIngress(aeronCtx.aeronDirectoryName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("Started Aeron gateway HTTP health server on port {}", app.port());
        return app.port();
    }

    private static void startIngress(String aeronDir) {
        gatewayAeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronDir));

        var requestEndpoint = envOrDefault("GATEWAY_REQUEST_ENDPOINT", "0.0.0.0:" + DEFAULT_REQUEST_PORT);
        var responseControlEndpoint = envOrDefault("GATEWAY_RESPONSE_CONTROL_ENDPOINT",
                DNSUtils.getThisHostName() + ":" + DEFAULT_RESPONSE_CONTROL_PORT);

        var egressWriter = new GatewayResponseWriter();
        var ingressAgent = new GatewayIngressAgent(gatewayAeron, requestEndpoint, responseControlEndpoint,
                REQUEST_STREAM_ID, RESPONSE_STREAM_ID, cache, cacheSubs, countersSubs, egressWriter);

        log.info("Gateway ingress request endpoint {}, response control endpoint {}", requestEndpoint, responseControlEndpoint);
        ingressAgentRunner = new AgentRunner(GatewayIdleStrategies.ingressAgentIdleStrategy.get(),
                new RethrowingErrorHandler(), null, ingressAgent);
        AgentRunner.startOnThread(ingressAgentRunner);
    }

    private static CacheClientFactory getCacheClientFactory() {
        ServiceLoader<CacheClientFactory> service = ServiceLoader.load(CacheClientFactory.class);
        Optional<CacheClientFactory> first = service.findFirst();
        return first.orElseThrow(() -> new IllegalStateException("No CacheClientFactory found."));
    }

    private static void buildClusterConnection(String egressIP, String ingressEndpoints, String aeronDirectory) {
        ReconnectingAeronCache reconnectingCache = new ReconnectingAeronCache(egressIP, ingressEndpoints, client,
                "GatewayClient", aeronDirectory, clusterConnected::set);
        reconnectingCache.connect();
        cache = reconnectingCache;
    }

    private static void buildUnclusteredConnection(Aeron aeron, String requestPubHost) {
        var requestPublicationChannel = "aeron:udp?endpoint=" + requestPubHost + ":7008|alias=AC-unclustered-requests";
        var requestPublication = aeron.addPublication(requestPublicationChannel, 1);

        var hostname = DNSUtils.getThisHostName();
        var responseSubscriptionChannel = "aeron:udp?endpoint=" + hostname + ":7007|alias=AC-unclustered-responses";
        var responseSubscription = aeron.addSubscription(responseSubscriptionChannel, 2);

        cache = new AeronCache() {
            @Override
            public void sendKeepAlive() {
            }

            @Override
            public int pollEgress() {
                return 0;
            }

            @Override
            public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int length) {
                long res;
                while ((res = requestPublication.offer(msgBuffer, msgBufferOffset, length)) < 0) {
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
                -> egressListener.onMessage(header.sessionId(), System.currentTimeMillis(), buffer, offset, length, header);

        Agent serverAgent = new Agent() {
            @Override
            public int doWork() {
                return responseSubscription.poll(egressFragmentHandler, 10);
            }

            @Override
            public String roleName() {
                return "AC-Unclustered-requests-listener";
            }
        };

        IdleStrategy unclusteredAgentIdleStrategy = GatewayIdleStrategies.unclusteredIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(unclusteredAgentIdleStrategy, Throwable::printStackTrace, null, serverAgent);
        AgentRunner.startOnThread(serverAgentRunner);
        clusterConnected.set(true);
    }

    private static Javalin startHTTPServer(int port) {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "aeron-cache-gateway");

        return Javalin.create()
                .get(LIVENESS, ctx -> ctx.status(200).result("OK"))
                .get(READINESS, ctx -> {
                    if (cache != null && cache.isConnected() && clusterConnected.get()) {
                        ctx.status(200).result("READY");
                    } else {
                        ctx.status(503).result("NOT_READY");
                    }
                })
                .get("/prometheus", ctx -> ctx.contentType(PROMETHEUS_CONTENT_TYPE).result(registry.scrape()))
                .start(port);
    }

    private static String envOrDefault(String name, String defaultValue) {
        var value = System.getenv(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
