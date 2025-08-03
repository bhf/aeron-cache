package com.bhf.aeroncache.handlers;

import io.aeron.cluster.codecs.EventCode;

/**
 * Handle session events associated with the cluster session.
 */
public interface ClusterSessionEventHandler {
    void handleSessionEvent(long correlationId, long clusterSessionId, long leadershipTermId, int leaderMemberId, EventCode code, String detail);

    void handleNewLeader(long clusterSessionId, long leadershipTermId, int leaderMemberId, String ingressEndpoints);
}
