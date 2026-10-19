package com.bhf.aeroncache.http.responses;

import java.util.List;

/**
 * Result of a cluster-wide snapshot-and-purge fanned out across every node by the HTTP server.
 *
 * <p>A single node takes the snapshot (it is cluster-wide and leader-coordinated, so one is enough),
 * then every node purges its own local archive. The per-node {@link RecordingPurgeResponse}s are
 * aggregated here: {@link #reclaimedBytes} is the sum across nodes and {@link #success} is true only
 * when every node succeeded. The {@link #nodes} breakdown preserves each node's individual result.
 *
 * @param success        whether the snapshot and every node's purge completed without error.
 * @param reclaimedBytes total bytes reclaimed on disk across all nodes.
 * @param message        human readable summary.
 * @param nodes          the per-node purge results, in the order the nodes were contacted.
 */
public record AggregatedPurgeResponse(
        boolean success,
        long reclaimedBytes,
        String message,
        List<NodePurgeResult> nodes) {

    /**
     * The purge outcome for a single cluster node.
     *
     * @param host           the clustertools sidecar host that was contacted.
     * @param success        whether this node's purge (and, for the first node, the snapshot) succeeded.
     * @param reclaimedBytes bytes reclaimed on disk on this node.
     * @param message        human readable detail from this node, or the failure reason.
     */
    public record NodePurgeResult(
            String host,
            boolean success,
            long reclaimedBytes,
            String message) {
    }
}
