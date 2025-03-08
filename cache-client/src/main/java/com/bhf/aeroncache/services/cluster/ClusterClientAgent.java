package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import io.aeron.cluster.client.AeronCluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

/**
 * An {@link Agent} implementation of an AeronCache Client that is run
 * via an {@link org.agrona.concurrent.AgentRunner}.
 */
@Log4j2
@RequiredArgsConstructor
public class ClusterClientAgent implements Agent {

    final AeronCluster cluster;
    final ManyToOneRingBuffer rb;
    private final int KEEPALIVE_INTERVAL = 200;
    long lastKeepAlive = 0;

    @Override
    public void onStart() {
        log.info("Starting cluster client agent");
        Agent.super.onStart();
    }

    /**
     * The core duty cycle of the Agent. Checks the request queue
     * for outbound requests, handles heartbeats and also polling
     * the egress for messages from the cluster.
     * @return
     * @throws Exception
     */
    @Override
    public int doWork() throws Exception {
        handleKeepAlive(cluster);

        // process the ManyToOneRingbuffer and offer messages to the cluster
        processInboundMessages(rb);
        return 0;
    }

    private void processInboundMessages(ManyToOneRingBuffer rb) {
        rb.read(new MessageHandler() {
            @Override
            public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length) {
                cluster.offer(buffer, index, length);
            }
        });
    }

    private void handleKeepAlive(AeronCluster cluster) {
        long now = System.currentTimeMillis();

        if (now > lastKeepAlive + KEEPALIVE_INTERVAL) {
            cluster.sendKeepAlive();
            lastKeepAlive = now;
        }
    }

    @Override
    public void onClose() {
        log.info("Closing cluster client agent");
        Agent.super.onClose();
    }

    @Override
    public String roleName() {
        return "AeronCache-Client-Agent";
    }
}
