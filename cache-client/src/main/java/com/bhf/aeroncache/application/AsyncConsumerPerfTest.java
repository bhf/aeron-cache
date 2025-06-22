package com.bhf.aeroncache.application;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.messages.OperationStatus;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cache.AeronCacheClusterListener;
import com.bhf.aeroncache.services.cache.CacheClientAgent;
import com.bhf.aeroncache.services.cache.CacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.ObservingCacheRequestPublisher;
import com.bhf.aeroncache.services.cache.impl.RBCacheRequestPublisher;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.utils.DNSUtils;
import com.bhf.aeroncache.utils.RingBufferUtils;
import io.aeron.cluster.client.AeronCluster;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Use the {@link Consumer} based approach for sending messages to the cache async
 * with SBE encoding being done on the {@link org.agrona.concurrent.Agent} thread.
 */
public class AsyncConsumerPerfTest {

    public static void main(String[] args) {
        ManyToOneRingBuffer rb = RingBufferUtils.buildRingbuffer(512 * 2);
        CacheRequestPublisher rbPublisher = new RBCacheRequestPublisher(rb);
        var observingPublisher = new ObservingCacheRequestPublisher(rbPublisher);

        var idleStrategy = new BackoffIdleStrategy();

        var client = new AeronCacheClusterListener();
        client.setCacheResultsCallbacks(observingPublisher);

        var podName = System.getenv("POD_ADDRESS");
        var allHosts = System.getenv("CLUSTER_ADDRESSES");

        System.out.println("POD_ADDRESS=" + podName);
        System.out.println("CLUSTER_ADDRESSES=" + allHosts);

        var egressIP = DNSUtils.getThisHostName();
        var hostArray = List.of(allHosts.split(","));
        var ingressEndpoints = ClusterUtils.ingressEndpoints(hostArray);

        var aeronCluster = ClusterUtils.buildClusterConnection(egressIP, ingressEndpoints, client);

        var agent = getCacheClientAgent(aeronCluster, rb, idleStrategy);
        var errorHandler = ClusterUtils.getAgentRunnerErrorHandler(aeronCluster);
        var errorCounter = ClusterUtils.getAgentErrorCounter(aeronCluster);
        AgentRunner runner = new AgentRunner(new YieldingIdleStrategy(), errorHandler, errorCounter, agent);
        AgentRunner.startOnThread(runner);

        runTest(observingPublisher);

        while (true) {

        }
    }

    private static CacheClientAgent getCacheClientAgent(AeronCluster aeronCluster, ManyToOneRingBuffer rb, BackoffIdleStrategy idleStrategy) {
        var cluster = new AeronCache() {
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
        };

        var agent = new CacheClientAgent(cluster, rb, idleStrategy, new ClusterMessagePublisher(cluster));
        return agent;
    }

    private static void runTest(ObservingCacheRequestPublisher observingPublisher) {
        Consumer<CreateCacheResult<ReusableLong>> consumer = result -> System.out.println("Created cache " + result.getCacheId() + ", status=" + result.getStatus());
        observingPublisher.sendCreateCache(UUID.randomUUID().toString(), 123L, consumer);

        AtomicLong count = new AtomicLong();
        AtomicLong errorCount = new AtomicLong();
        AtomicLong start = new AtomicLong(System.currentTimeMillis());

        while (true) {
            Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> addEntryConsumer = reusableLongReusableStringAddCacheEntryResult -> {
                if (reusableLongReusableStringAddCacheEntryResult.getStatus() != OperationStatus.SUCCESS) {
                    var errors = errorCount.incrementAndGet();
                    System.out.println("TOTAL ERRORS: " + errors);
                }

                long v = count.incrementAndGet();

                if (v % 1000 == 0) {
                    long now = System.currentTimeMillis();
                    long dur = now - start.get();
                    double per = dur / 1000.0;
                    start.set(now);
                    System.out.println(v + "," + per);
                }
            };
            observingPublisher.addCacheEntry(UUID.randomUUID().toString(), 123L, UUID.randomUUID().toString(), UUID.randomUUID().toString(), addEntryConsumer);
        }
    }
}
