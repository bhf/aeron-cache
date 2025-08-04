package com.bhf.aeroncache.application.unclustered;

import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import io.aeron.*;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

@RequiredArgsConstructor
@Log4j2
public class UnclusteredServiceAgent implements Agent {

    private final Aeron aeron;
    private final SBEDecodingCacheClusterService service;

    private final String httpRequestsChannel;
    private final String wsRequestsChannel;
    private final int requestsStream;

    private final String httpResponseChannel;
    private final String wsResponseChannel;
    private final int responseStream;

    private Subscription httpRequestsSubscription;
    private Publication httpResponsePublication;

    private Subscription wsRequestsSubscription;
    private Publication wsResponsePublication;
    private FragmentHandler fragmentHandler;
    private FragmentAssembler assembler;
    private long msgCount = 0;

    @Override
    public void onStart() {
        log.info("Starting unclustered Aeron Cache");

        httpRequestsSubscription = aeron.addSubscription(httpRequestsChannel, requestsStream);
        wsRequestsSubscription = aeron.addSubscription(wsRequestsChannel, requestsStream);
        while (!httpRequestsSubscription.isConnected() && !wsRequestsSubscription.isConnected()) {
            aeron.context().idleStrategy().idle();
        }

        log.info("Request subscription connected, http: {}, ws: {}", httpRequestsSubscription.isConnected(),
                wsRequestsSubscription.isConnected());

        httpResponsePublication = aeron.addPublication(httpResponseChannel, responseStream);
        wsResponsePublication = aeron.addPublication(wsResponseChannel, responseStream);
        while (!httpResponsePublication.isConnected() && !wsResponsePublication.isConnected()) {
            aeron.context().idleStrategy().idle();
        }

        log.info("Response publication connected, http: {}, ws: {}", httpResponsePublication.isConnected(),
                wsResponsePublication.isConnected());

        ClientSession session = getClientSession();
        fragmentHandler = (buffer, offset, length, header) -> service.onSessionMessage(session,
                msgCount++, buffer, offset, length, header);

        assembler = new FragmentAssembler(fragmentHandler);
    }

    private ClientSession getClientSession() {
        return new ClientSession() {
            @Override
            public long id() {
                return httpResponsePublication.sessionId();
            }

            @Override
            public int responseStreamId() {
                return httpResponsePublication.streamId();
            }

            @Override
            public String responseChannel() {
                return httpResponsePublication.channel();
            }

            @Override
            public byte[] encodedPrincipal() {
                return new byte[0];
            }

            @Override
            public void close() {
                httpResponsePublication.close();
            }

            @Override
            public boolean isClosing() {
                return httpResponsePublication.isClosed();
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                while(wsResponsePublication.offer(buffer, offset, length)<0){
                    aeron.context().idleStrategy().idle();
                }

                return httpResponsePublication.offer(buffer, offset, length);
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                return httpResponsePublication.offer(vectors);
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                return httpResponsePublication.tryClaim(length, bufferClaim);
            }
        };
    }

    @Override
    public int doWork() throws Exception {
        wsRequestsSubscription.poll(assembler, Integer.MAX_VALUE);
        return httpRequestsSubscription.poll(assembler, Integer.MAX_VALUE);
    }

    @Override
    public void onClose() {
        Agent.super.onClose();
    }

    @Override
    public String roleName() {
        return "Unclustered-AC-Agent";
    }
}
