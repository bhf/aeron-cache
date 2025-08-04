package com.bhf.aeroncache.application.unclustered;

import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
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
import org.agrona.concurrent.SleepingIdleStrategy;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Run a cache without any clustering.
 */
public class SingleNodeApplication {

    public static void main(String[] args) {
        final IdleStrategy idleStrategy = new SleepingIdleStrategy();

        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)
                .threadingMode(ThreadingMode.SHARED);
        final MediaDriver mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
                .aeronDirectoryName(mediaDriver.aeronDirectoryName());
        final Aeron aeron = Aeron.connect(aeronCtx);

        final SBEDecodingCacheClusterService service = new SBEDecodingCacheClusterService("0", new NoOpTracingService());
        Cluster cluster = getCluster(aeron);
        service.onStart(cluster, null);

        final var httpRequests = "aeron:udp?endpoint=:8008|alias=AC-unclustered-http-requests";
        final var wsRequests = "aeron:udp?endpoint=:7008|alias=AC-unclustered-ws-requests";
        final int requestStream = 1;

        var httpResponseHost = System.getenv("HTTP_RESPONSE_PUB_HOST");
        var wsResponseHost = System.getenv("WS_RESPONSE_PUB_HOST");

        final var httpResponses = "aeron:udp?endpoint="+httpResponseHost+":8007|alias=AC-unclustered-http-responses";
        final var wsResponses = "aeron:udp?endpoint="+wsResponseHost+":7007|alias=AC-unclustered-ws-responses";
        final int responsesStream = 2;

        final UnclusteredServiceAgent serverAgent = new UnclusteredServiceAgent(aeron, service, httpRequests,
                wsRequests, requestStream,
                httpResponses, wsResponses, responsesStream);
        final AgentRunner serverAgentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace,
                null, serverAgent);
        AgentRunner.startOnThread(serverAgentRunner);
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
