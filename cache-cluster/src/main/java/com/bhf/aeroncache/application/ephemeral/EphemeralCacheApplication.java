package com.bhf.aeroncache.application.ephemeral;

import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import io.aeron.Aeron;
import io.aeron.DirectBufferVector;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;

import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Run a cache without any clustering.
 */
public class EphemeralCacheApplication {

    /** Default request subscription endpoint (bind all interfaces). */
    public static final String DEFAULT_REQUEST_ENDPOINT = "0.0.0.0:8075";
    /** Default response control port; the host defaults to this node's hostname. */
    public static final int DEFAULT_RESPONSE_CONTROL_PORT = 8076;
    /** Default request/response stream ids (distinct from the gateway's 100/101). */
    public static final int DEFAULT_REQUEST_STREAM_ID = 200;
    public static final int DEFAULT_RESPONSE_STREAM_ID = 201;

    public static void main(String[] args) {

        boolean dynamicCacheCreation = parseDynamicCacheCreation();

        var hostname = DNSUtils.getThisHostName();
        System.out.println("Single node cache hostname: "+hostname);

        final String requestEndpoint = System.getenv().getOrDefault(
                "EPHEMERAL_REQUEST_ENDPOINT", DEFAULT_REQUEST_ENDPOINT);
        final String responseControlEndpoint = System.getenv().getOrDefault(
                "EPHEMERAL_RESPONSE_CONTROL_ENDPOINT", hostname + ":" + DEFAULT_RESPONSE_CONTROL_PORT);
        final int requestStreamId = parseIntEnv("EPHEMERAL_REQUEST_STREAM_ID", DEFAULT_REQUEST_STREAM_ID);
        final int responseStreamId = parseIntEnv("EPHEMERAL_RESPONSE_STREAM_ID", DEFAULT_RESPONSE_STREAM_ID);

        System.out.println("Ephemeral request endpoint: "+requestEndpoint);
        System.out.println("Ephemeral response control endpoint: "+responseControlEndpoint);

        start(requestEndpoint, responseControlEndpoint, requestStreamId, responseStreamId, dynamicCacheCreation);
    }

    /**
     * Launch an unclustered (ephemeral) cache in-process, launching a dedicated embedded media driver
     * and starting the service agent on its own thread.
     * <p>
     * Clients connect over Aeron response channels ({@code control-mode=response}): a single request
     * subscription bound to {@code requestEndpoint} accepts any number of clients, and each client's
     * responses are routed back over a per-session response publication advertised on
     * {@code responseControlEndpoint}. Response publications are created on demand as clients connect,
     * so this method returns as soon as the agent thread is launched.
     *
     * @param requestEndpoint          endpoint the request subscription binds to (e.g. {@code 0.0.0.0:8075}).
     * @param responseControlEndpoint  the response control endpoint advertised to clients (host:port).
     * @param requestStreamId          the request stream id.
     * @param responseStreamId         the response stream id.
     * @param dynamicCacheCreation     whether caches may be created dynamically on first write.
     * @return the {@link AgentRunner} driving the cache service agent.
     */
    public static AgentRunner start(String requestEndpoint,
                                    String responseControlEndpoint,
                                    int requestStreamId,
                                    int responseStreamId,
                                    boolean dynamicCacheCreation) {

        // LAUNCH_EMBEDDED=false attaches to an external media driver at AERON_DIR (default unset
        // -> embedded), matching the other apps.
        final boolean launchEmbedded =
                Boolean.parseBoolean(System.getenv().getOrDefault("LAUNCH_EMBEDDED", "true"));
        final String aeronDir;
        if (launchEmbedded) {
            final MediaDriver.Context mediaDriverCtx = ClusterUtils.applyConfiguredTermLength(new MediaDriver.Context()
                    .dirDeleteOnStart(true)
                    .dirDeleteOnShutdown(true)
                    .threadingMode(ThreadingMode.SHARED));
            final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);
            aeronDir = mediaDriver.aeronDirectoryName();
        } else {
            aeronDir = System.getenv("AERON_DIR");
        }

        final Aeron.Context aeronCtx = new Aeron.Context()
                .aeronDirectoryName(aeronDir);
        final Aeron aeron = Aeron.connect(aeronCtx);

        final var cacheManagerFactory = getCacheManagerFactory();

        final SBEDecodingCacheClusterService service = new SBEDecodingCacheClusterService("0",
                new NoOpTracingService(), cacheManagerFactory, dynamicCacheCreation);
        final EphemeralTimerService timerService = new EphemeralTimerService(service::onTimerEvent);
        final EphemeralSessionRegistry registry =
                new EphemeralSessionRegistry(aeron, responseControlEndpoint, responseStreamId);
        Cluster cluster = getCluster(aeron, timerService, registry);
        service.onStart(cluster, null);

        final EphemeralCacheServiceAgent serverAgent = new EphemeralCacheServiceAgent(aeron, service, timerService,
                requestEndpoint, responseControlEndpoint, requestStreamId, responseStreamId, registry);
        final IdleStrategy idleStrategy = EphemeralCacheIdleStrategies.unclusteredAgentIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace,
                null, serverAgent);
        AgentRunner.startOnThread(serverAgentRunner);
        return serverAgentRunner;
    }

    private static int parseIntEnv(String name, int defaultValue) {
        var value = System.getenv(name);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.out.println("Couldn't parse value of "+name+" as int, using default "+defaultValue);
            }
        }
        return defaultValue;
    }

    private static boolean parseDynamicCacheCreation() {
        var useDynamicCacheCreation = System.getenv("DYNAMIC_CACHE_CREATION");
        if (useDynamicCacheCreation != null) {
            try {
                return Boolean.parseBoolean(useDynamicCacheCreation);
            } catch (Exception e) {
                System.out.println("Couldn't parse value of DYNAMIC_CACHE_CREATION as boolean");
            }
        }
        return false;
    }

    private static CacheManagerFactory getCacheManagerFactory() {
        ServiceLoader<CacheManagerFactory> service = ServiceLoader.load(CacheManagerFactory.class);
        Optional<CacheManagerFactory> first = service.findFirst();

        if (first.isPresent()) {
            return first.get();
        } else {
            throw new IllegalStateException("No CacheManagerFactory found.");
        }
    }

    private static Cluster getCluster(Aeron aeron, EphemeralTimerService timerService,
                                      EphemeralSessionRegistry registry) {
        Cluster cluster = new Cluster() {
            @Override
            public int memberId() {
                return 0;
            }

            @Override
            public Role role() {
                return null;
            }

            @Override
            public long logPosition() {
                return 0;
            }

            @Override
            public Aeron aeron() {
                return null;
            }

            @Override
            public ClusteredServiceContainer.Context context() {
                return null;
            }

            @Override
            public ClientSession getClientSession(long clusterSessionId) {
                return registry.get(clusterSessionId);
            }

            @Override
            public Collection<ClientSession> clientSessions() {
                return registry.values();
            }

            @Override
            public void forEachClientSession(Consumer<? super ClientSession> action) {
                registry.values().forEach(action);
            }

            @Override
            public boolean closeClientSession(long clusterSessionId) {
                registry.remove(clusterSessionId);
                return true;
            }

            @Override
            public long time() {
                return System.currentTimeMillis();
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public boolean scheduleTimer(long correlationId, long deadline) {
                return timerService.scheduleTimer(correlationId, deadline);
            }

            @Override
            public boolean cancelTimer(long correlationId) {
                return timerService.cancelTimer(correlationId);
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                return 0;
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                return 0;
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                return 0;
            }

            @Override
            public IdleStrategy idleStrategy() {
                return aeron.context().idleStrategy();
            }
        };
        return cluster;
    }
}
