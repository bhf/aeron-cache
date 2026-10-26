package com.bhf.aeroncache.http.responses;

import java.util.List;

/**
 * The Aeron media driver counters of every node, gathered by the HTTP gateway from each node's
 * clustertools sidecar so the UI can render an observability dashboard from a single call.
 *
 * @param mode      the gateway's cache mode, {@code CLUSTERED} (Raft) or {@code EPHEMERAL}.
 * @param nodeCount the number of nodes the gateway knows about. A clustered gateway with a single
 *                  node is a monolith / single node deployment.
 * @param nodes     one entry per node, in configured host order.
 */
public record AeronCountersFanoutResponse(String mode, int nodeCount, List<NodeCounters> nodes) {

    /**
     * The counters scraped from one node.
     *
     * @param host     the node's clustertools host.
     * @param reachable whether the counters were scraped successfully.
     * @param counters the node's driver counters, or null when not reachable.
     */
    public record NodeCounters(String host, boolean reachable, AeronCountersResponse counters) {
    }
}
