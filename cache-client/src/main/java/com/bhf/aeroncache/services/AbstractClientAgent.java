package com.bhf.aeroncache.services;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.RingBuffer;

/**
 * An {@link Agent} abstraction of an AeronCache Client that is run
 * via an {@link org.agrona.concurrent.AgentRunner}.
 */
@Log4j2
@RequiredArgsConstructor
public abstract class AbstractClientAgent implements Agent {

    @Getter
    final AeronCache cluster;
    final RingBuffer rb;
    final IdleStrategy idleStrategy;

    @Getter
    final ClusterMessagePublisher<String, String, String> cachePublisher;
    @Getter
    final ClusterMessagePublisher<String, String, Long> counterOpsPublisher;

    final String roleName;
    volatile boolean isEnabled = true;

    @Getter
    private final int KEEPALIVE_INTERVAL = 200;

    @Getter
    @Setter
    long lastKeepAlive = 0;

    @Override
    public void onStart() {
        log.info("Starting cache client agent");
        Agent.super.onStart();
    }

    /**
     * The core duty cycle of the Agent. Checks the request queue
     * for requests to be encoded for the cache, handles heartbeats
     * and also polling the egress for messages from the cluster.
     *
     * @return
     * @throws Exception
     */
    @Override
    public int doWork() throws Exception {
        while (isEnabled) {
            runSingleCycle();
        }

        return 0;
    }

    public void runSingleCycle() {
        handleKeepAlive(cluster);
        processInboundMessages(rb);
        cluster.pollEgress();
        idleStrategy.idle();
    }

    public abstract void processInboundMessages(RingBuffer rb);

    private void handleKeepAlive(AeronCache cluster) {
        long now = System.currentTimeMillis();

        if (now > lastKeepAlive + KEEPALIVE_INTERVAL) {
            cluster.sendKeepAlive();
            lastKeepAlive = now;
        }
    }

    @Override
    public void onClose() {
        log.info("Closing agent for {}", roleName);
        Agent.super.onClose();
    }

    @Override
    public String roleName() {
        return roleName;
    }
}
