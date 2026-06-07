package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.services.AbstractClientAgent;
import com.bhf.aeroncache.services.cluster.impl.ClusterMessagePublisher;
import io.aeron.cluster.client.AeronCluster;
import lombok.extern.log4j.Log4j2;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ringbuffer.RingBuffer;

/**
 * An {@link Agent} implementation of an AeronCache Client that is run
 * via an {@link org.agrona.concurrent.AgentRunner}.
 * <p>
 * Accepts messages pre-encoded in a format that the cluster is expecting and
 * offers them to an {@link AeronCluster} instance.
 * <p>
 * A good option when you're not too worried about head of line blocking on
 * the publishing side (you take the hit of encoding the payload before it hits the agent thread)
 * and want to minimise the work this agent does e.g. with a lower KEEPALIVE_INTERVAL.
 */
@Log4j2
public class ClusterClientAgent extends AbstractClientAgent {

    public ClusterClientAgent(AeronCache cluster, RingBuffer rb, IdleStrategy idleStrategy, ClusterMessagePublisher publisher, String roleName) {
        super(cluster, rb, idleStrategy, publisher, roleName);
    }

    public void processInboundMessages(RingBuffer rb) {
        rb.read((msgTypeId, buffer, index, length) -> getCluster().offer(buffer, index, length));
    }

}
