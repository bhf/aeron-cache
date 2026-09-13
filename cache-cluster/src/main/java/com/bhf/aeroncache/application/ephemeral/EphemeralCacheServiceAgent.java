package com.bhf.aeroncache.application.ephemeral;

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
public class EphemeralCacheServiceAgent implements Agent {

    private final Aeron aeron;
    private final SBEDecodingCacheClusterService service;
    private final EphemeralTimerService timerService;

    private final String httpRequestsChannel;
    private final String wsRequestsChannel;
    private final String sseRequestsChannel;
    private final int requestsStream;

    private final String httpResponseChannel;
    private final String wsResponseChannel;
    private final String sseResponseChannel;
    private final int responseStream;

    private Subscription httpRequestsSubscription;
    private Publication httpResponsePublication;

    private Subscription wsRequestsSubscription;
    private Publication wsResponsePublication;

    private Subscription sseRequestsSubscription;
    private Publication sseResponsePublication;

    private FragmentHandler fragmentHandler;
    private FragmentAssembler assembler;
    private long msgCount = 0;

    /**
     * Upper bound on how long a response offer will wait for the requesting interface's response
     * publication to connect before falling back to best-effort delivery. This closes the fresh-start
     * race where an interface's first request (e.g. a subscribe) is handled before its response
     * publication has finished connecting, without letting a vanished peer wedge the agent thread.
     */
    private static final long AWAIT_RESPONSE_CONNECT_NANOS = 5_000_000_000L;

    @Override
    public void onStart() {
        log.info("Starting unclustered Aeron Cache");

        initialiseRequests();
        log.info("Request subscription connected, http: {}, ws: {}", httpRequestsSubscription.isConnected(),
                wsRequestsSubscription.isConnected());

        initialiseResponses();
        log.info("Response publication connected, http: {}, ws: {}, sse: {}", httpResponsePublication.isConnected(),
                wsResponsePublication.isConnected(), sseResponsePublication.isConnected());

        ClientSession session = getClientSession();
        fragmentHandler = (buffer, offset, length, header) -> service.onSessionMessage(session,
                msgCount++, buffer, offset, length, header);

        assembler = new FragmentAssembler(fragmentHandler);
    }

    /**
     * Initialise the response publications and wait for at least one of them to be connected so that we
     * can start serving. We deliberately do not wait for all three: an ephemeral cache may be paired
     * with only a subset of interfaces (for example the gateway embedded tests wire only the WS
     * transport), and gating startup on every publication would hang forever when one has no peer. The
     * per-request delivery in {@link #getClientSession()}'s offer waits (bounded) for the requesting
     * interface's own publication, which closes the startup race for that interface without this global
     * gate.
     */
    private void initialiseResponses() {
        httpResponsePublication = aeron.addPublication(httpResponseChannel, responseStream);
        wsResponsePublication = aeron.addPublication(wsResponseChannel, responseStream);
        sseResponsePublication = aeron.addPublication(sseResponseChannel, responseStream);

        while (!httpResponsePublication.isConnected() && !wsResponsePublication.isConnected()
                && !sseResponsePublication.isConnected()) {
            aeron.context().idleStrategy().idle();
        }
    }

    /**
     * Initialise the request subscriptions and wait for at least
     * one of them to be connected so that we can accept requests.
     */
    private void initialiseRequests() {
        httpRequestsSubscription = aeron.addSubscription(httpRequestsChannel, requestsStream);
        wsRequestsSubscription = aeron.addSubscription(wsRequestsChannel, requestsStream);
        sseRequestsSubscription = aeron.addSubscription(sseRequestsChannel, requestsStream);

        while (!httpRequestsSubscription.isConnected() && !wsRequestsSubscription.isConnected()
                && !sseRequestsSubscription.isConnected()) {
            aeron.context().idleStrategy().idle();
        }
    }

    private Subscription currentlyPollingSubscription;
    private Publication matchingResponsePublication;

    private ClientSession getClientSession() {
        return new ClientSession() {
            @Override
            public long id() {
                return matchingResponsePublication.sessionId();
            }

            @Override
            public int responseStreamId() {
                return matchingResponsePublication.streamId();
            }

            @Override
            public String responseChannel() {
                return matchingResponsePublication.channel();
            }

            @Override
            public byte[] encodedPrincipal() {
                return new byte[0];
            }

            @Override
            public void close() {
                httpResponsePublication.close();
                wsResponsePublication.close();
                sseResponsePublication.close();
            }

            @Override
            public boolean isClosing() {
                return currentlyPollingSubscription.isClosed();
            }

            @Override
            public long offer(DirectBuffer buffer, int offset, int length) {
                // Make sure the response reaches the interface that made this request even if its
                // response publication is still connecting on a fresh start - otherwise that interface's
                // first message (typically a subscribe confirmation) is silently dropped by the
                // isConnected() guards below while later traffic gets through. Bounded so an interface
                // that never connects (e.g. one not present in this deployment) cannot wedge the agent.
                final Publication requester = matchingResponsePublication;
                if (requester != null && !requester.isConnected()) {
                    final long deadlineNs = System.nanoTime() + AWAIT_RESPONSE_CONNECT_NANOS;
                    while (!requester.isConnected() && System.nanoTime() < deadlineNs) {
                        aeron.context().idleStrategy().idle();
                    }
                }

                long responseCodeHttp = 0;
                if (httpResponsePublication.isConnected()) {
                    while ((responseCodeHttp = httpResponsePublication.offer(buffer, offset, length)) < 0) {
                        aeron.context().idleStrategy().idle();
                    }
                }

                long responseCodeWs = 0;
                if (wsResponsePublication.isConnected()) {
                    while ((responseCodeWs = wsResponsePublication.offer(buffer, offset, length)) < 0) {
                        aeron.context().idleStrategy().idle();
                    }
                }

                long responseCodeSse = 0;
                if (sseResponsePublication.isConnected()) {
                    while ((responseCodeSse = sseResponsePublication.offer(buffer, offset, length)) < 0) {
                        aeron.context().idleStrategy().idle();
                    }
                }

                return responseCodeHttp+responseCodeWs+responseCodeSse;
            }

            @Override
            public long offer(DirectBufferVector[] vectors) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long tryClaim(int length, BufferClaim bufferClaim) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int doWork() throws Exception {

        int res = 0;

        res += timerService.poll(System.currentTimeMillis());

        if (wsRequestsSubscription.isConnected()) {
            currentlyPollingSubscription = wsRequestsSubscription;
            matchingResponsePublication = wsResponsePublication;
            res += wsRequestsSubscription.poll(assembler, 10);
        }

        if (httpRequestsSubscription.isConnected()) {
            currentlyPollingSubscription = httpRequestsSubscription;
            matchingResponsePublication = httpResponsePublication;
            res += httpRequestsSubscription.poll(assembler, 10);
        }

        if (sseRequestsSubscription.isConnected()) {
            currentlyPollingSubscription = sseRequestsSubscription;
            matchingResponsePublication = sseResponsePublication;
            res += sseRequestsSubscription.poll(assembler, 10);
        }

        return res;
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
