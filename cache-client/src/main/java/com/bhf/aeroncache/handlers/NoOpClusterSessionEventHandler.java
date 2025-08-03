package com.bhf.aeroncache.handlers;

import io.aeron.cluster.codecs.EventCode;

public class NoOpClusterSessionEventHandler implements ClusterSessionEventHandler {
    @Override
    public void handleSessionEvent(long correlationId, long clusterSessionId, long leadershipTermId,
                                   int leaderMemberId, EventCode code, String detail) {

    }

    @Override
    public void handleNewLeader(long clusterSessionId, long leadershipTermId, int leaderMemberId, String ingressEndpoints) {

    }
}
