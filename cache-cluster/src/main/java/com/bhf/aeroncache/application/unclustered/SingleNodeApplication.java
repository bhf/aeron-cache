package com.bhf.aeroncache.application.unclustered;

import com.bhf.aeroncache.application.CacheSnapshotCodecUtils;
import com.bhf.aeroncache.services.cachemanager.BasicCacheManagerFactory;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.codecs.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.SupplierUtils;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Run a cache without any clustering.
 */
public class SingleNodeApplication {

    public static void main(String[] args) {

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)
                .threadingMode(ThreadingMode.SHARED);
        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        final Aeron aeron = Aeron.connect(aeronCtx);

        final var cacheManagerFactory = getCacheManager();

        final SBEDecodingCacheClusterService service = new SBEDecodingCacheClusterService("0",
                new NoOpTracingService(), cacheManagerFactory);
        Cluster cluster = getCluster(aeron);
        service.onStart(cluster, null);

        var hostname = DNSUtils.getThisHostName();
        System.out.println("Single node cache hostname: "+hostname);

        final var httpRequests = "aeron:udp?endpoint="+hostname+":8008|alias=AC-unclustered-http-requests";
        final var wsRequests = "aeron:udp?endpoint="+hostname+":7008|alias=AC-unclustered-ws-requests";
        final var sseRequests = "aeron:udp?endpoint="+hostname+":6008|alias=AC-unclustered-sse-requests";
        final int requestStream = 1;

        var httpResponseHost = System.getenv("HTTP_RESPONSE_PUB_HOST");
        var wsResponseHost = System.getenv("WS_RESPONSE_PUB_HOST");
        var sseResponseHost = System.getenv("SSE_RESPONSE_PUB_HOST");

        System.out.println("HTTP Response host: "+httpResponseHost);
        System.out.println("WS Response host: "+wsResponseHost);
        System.out.println("SSE Response host: "+sseResponseHost);

        final List<String> hostAddresses = List.of(httpResponseHost, wsResponseHost, sseResponseHost);

        for (int i = 0; i < hostAddresses.size(); i++) {
            DNSUtils.awaitDnsResolution(hostAddresses, i);
        }

        System.out.println("Finished DNS resolution on "+hostAddresses);

        final var httpResponses = "aeron:udp?endpoint="+httpResponseHost+":8007|alias=AC-unclustered-http-responses";
        final var wsResponses = "aeron:udp?endpoint="+wsResponseHost+":7007|alias=AC-unclustered-ws-responses";
        final var sseResponses = "aeron:udp?endpoint="+sseResponseHost+":6007|alias=AC-unclustered-sse-responses";
        final int responsesStream = 2;

        final UnclusteredServiceAgent serverAgent = new UnclusteredServiceAgent(aeron, service, httpRequests,
                wsRequests, sseRequests, requestStream,
                httpResponses, wsResponses, sseResponses, responsesStream);
        final IdleStrategy idleStrategy = SingleNodeIdleStrategies.unclusteredAgentIdleStrategy.get();
        final AgentRunner serverAgentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace,
                null, serverAgent);
        AgentRunner.startOnThread(serverAgentRunner);
    }

    private static CacheManagerFactory<ReusableString, ReusableString, ReusableString> getCacheManager() {
        var encoder = new ReusableStringCacheResponseEncoder();
        var decoder = new ReusableStringCacheRequestDecoder();
        return new BasicCacheManagerFactory<>(SupplierUtils.stringSupplier,
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.mapSupplier,
                CacheSnapshotCodecUtils.getCacheIdSnapshotCodec(), CacheSnapshotCodecUtils.getCacheEntrySnapshotCodec(), encoder, decoder);
    }

    private static Cluster getCluster(Aeron aeron) {
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
                return null;
            }

            @Override
            public Collection<ClientSession> clientSessions() {
                return null;
            }

            @Override
            public void forEachClientSession(Consumer<? super ClientSession> action) {

            }

            @Override
            public boolean closeClientSession(long clusterSessionId) {
                return false;
            }

            @Override
            public long time() {
                return 0;
            }

            @Override
            public TimeUnit timeUnit() {
                return null;
            }

            @Override
            public boolean scheduleTimer(long correlationId, long deadline) {
                return false;
            }

            @Override
            public boolean cancelTimer(long correlationId) {
                return false;
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
