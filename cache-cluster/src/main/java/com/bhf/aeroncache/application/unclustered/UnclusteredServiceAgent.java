package com.bhf.aeroncache.application.unclustered;

import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import io.aeron.Aeron;
import io.aeron.DirectBufferVector;
import io.aeron.Publication;
import io.aeron.Subscription;
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

    private static final String HTTP_SUBSCRIPTION_CHANNEL = "aeron:udp?endpoint=localhost:8008|alias=AC-unclustered" +
            "-http-requests";
    private static final String WS_SUBSCRIPTION_CHANNEL = "aeron:udp?endpoint=localhost:7008|alias=AC-unclustered-ws" +
            "-requests";
    private static final int SUBSCRIPTION_STREAM = 1;
    private static final String HTTP_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:8007|alias=AC-unclustered-http" +
            "-responses";
    private static final String WS_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:7007|alias=AC-unclustered-ws" +
            "-responses";
    private static final int RESPONSE_STREAM = 2;
    private final Aeron aeron;
    private final SBEDecodingCacheClusterService service;
    private Subscription httpRequestsSubscription;
    private Publication httpResponsePublication;

    private Subscription wsRequestsSubscription;
    private Publication wsResponsePublication;
    private FragmentHandler fragmentHandler;
    private long msgCount = 0;

    @Override
    public void onStart() {
        log.info("Starting unclustered Aeron Cache");

        httpRequestsSubscription = aeron.addSubscription(HTTP_SUBSCRIPTION_CHANNEL, SUBSCRIPTION_STREAM);
        wsRequestsSubscription = aeron.addSubscription(WS_SUBSCRIPTION_CHANNEL, SUBSCRIPTION_STREAM);
        while (!httpRequestsSubscription.isConnected() && !wsRequestsSubscription.isConnected()) {
            aeron.context().idleStrategy().idle();
        }

        log.info("Request subscription connected, http: {}, ws: {}", httpRequestsSubscription.isConnected(),
                wsRequestsSubscription.isConnected());

        httpResponsePublication = aeron.addPublication(HTTP_RESPONSE_CHANNEL, RESPONSE_STREAM);
        wsResponsePublication = aeron.addPublication(WS_RESPONSE_CHANNEL, RESPONSE_STREAM);
        while (!httpResponsePublication.isConnected() && !wsResponsePublication.isConnected()) {
            aeron.context().idleStrategy().idle();
        }

        log.info("Response publication connected, http: {}, ws: {}", httpResponsePublication.isConnected(),
                wsResponsePublication.isConnected());

        ClientSession session = getClientSession();
        fragmentHandler = (buffer, offset, length, header) -> service.onSessionMessage(session,
                msgCount++, buffer, offset, length, header);
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
        wsRequestsSubscription.poll(fragmentHandler, Integer.MAX_VALUE);
        return httpRequestsSubscription.poll(fragmentHandler, Integer.MAX_VALUE);
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
