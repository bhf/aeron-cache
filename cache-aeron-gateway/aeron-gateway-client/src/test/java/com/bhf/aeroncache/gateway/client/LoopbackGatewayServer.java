package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.BooleanType;
import com.bhf.aeroncache.gateway.messages.GatewayCommandDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayCommandResponseEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayEntriesEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayErrorEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStatsEncoder;
import com.bhf.aeroncache.gateway.messages.GatewayStreamUpdateEncoder;
import com.bhf.aeroncache.gateway.messages.GatewaySubscribeDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderEncoder;
import com.bhf.aeroncache.gateway.messages.OperationStatus;
import com.bhf.aeroncache.gateway.messages.UpdateEventType;
import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal in-process gateway server used to exercise the {@link GatewayClient} transport end to end.
 * <p>
 * It mirrors the production gateway's Aeron response-channel wiring (a single request subscription with
 * {@code response-endpoint}, and one {@code control-mode=response} publication per client image keyed by
 * {@link Image#correlationId()}) but replies with canned data rather than talking to a cluster, so the
 * test can assert the client's decode/dispatch behaviour without standing up a backend.
 * <p>
 * Runs on a single {@link org.agrona.concurrent.AgentRunner} thread. Responses are queued and only
 * offered once the per-session response publication is connected.
 */
class LoopbackGatewayServer implements Agent {

    private static final int FRAGMENT_LIMIT = 10;

    private final Aeron aeron;
    private final int requestStreamId;
    private final int responseStreamId;
    private final ChannelUriStringBuilder requestUriBuilder;
    private final ChannelUriStringBuilder responseUriBuilder;
    private final Long2ObjectHashMap<Publication> sessions = new Long2ObjectHashMap<>();
    private final Deque<PendingFrame> pending = new ArrayDeque<>();

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final GatewayCommandDecoder commandDecoder = new GatewayCommandDecoder();
    private final GatewaySubscribeDecoder subscribeDecoder = new GatewaySubscribeDecoder();
    private final GatewayCommandResponseEncoder commandResponseEncoder = new GatewayCommandResponseEncoder();
    private final GatewayEntriesEncoder entriesEncoder = new GatewayEntriesEncoder();
    private final GatewayStatsEncoder statsEncoder = new GatewayStatsEncoder();
    private final GatewayStreamUpdateEncoder streamUpdateEncoder = new GatewayStreamUpdateEncoder();
    private final GatewayErrorEncoder errorEncoder = new GatewayErrorEncoder();
    private final MutableDirectBuffer scratch = new ExpandableArrayBuffer(4096);
    private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);

    private Subscription subscription;

    LoopbackGatewayServer(Aeron aeron, String requestEndpoint, String responseControl,
                          int requestStreamId, int responseStreamId) {
        this.aeron = aeron;
        this.requestStreamId = requestStreamId;
        this.responseStreamId = responseStreamId;
        this.requestUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .endpoint(requestEndpoint)
                .responseEndpoint(responseControl);
        this.responseUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode("response")
                .controlEndpoint(responseControl);
    }

    @Override
    public int doWork() {
        int work = 0;
        if (subscription == null) {
            subscription = aeron.addSubscription(requestUriBuilder.build(), requestStreamId);
            work++;
        }
        work += drainPending();
        work += subscription.poll(fragmentAssembler, FRAGMENT_LIMIT);
        return work;
    }

    @Override
    public String roleName() {
        return "Loopback-Gateway-Server";
    }

    @Override
    public void onClose() {
        sessions.values().forEach(Publication::close);
        sessions.clear();
        if (subscription != null) {
            subscription.close();
        }
    }

    private int drainPending() {
        int work = 0;
        int remaining = pending.size();
        while (remaining-- > 0) {
            PendingFrame frame = pending.poll();
            if (frame == null) {
                break;
            }
            if (frame.publication.isConnected() && frame.publication.offer(frame.buffer, 0, frame.length) > 0) {
                work++;
            } else {
                pending.addLast(frame);
            }
        }
        return work;
    }

    private Publication responseFor(Image image) {
        final long correlationId = image.correlationId();
        Publication publication = sessions.get(correlationId);
        if (publication == null) {
            publication = aeron.addPublication(
                    responseUriBuilder.responseCorrelationId(correlationId).build(), responseStreamId);
            sessions.put(correlationId, publication);
        }
        return publication;
    }

    private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        final Image image = (Image) header.context();
        final Publication response = responseFor(image);

        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();
        final int blockLength = headerDecoder.blockLength();
        final int version = headerDecoder.version();
        final int bodyOffset = offset + headerDecoder.encodedLength();

        if (templateId == GatewayCommandDecoder.TEMPLATE_ID) {
            commandDecoder.wrap(buffer, bodyOffset, blockLength, version);
            handleCommand(response);
        } else if (templateId == GatewaySubscribeDecoder.TEMPLATE_ID) {
            subscribeDecoder.wrap(buffer, bodyOffset, blockLength, version);
            handleSubscribe(response);
        }
    }

    private void handleCommand(Publication response) {
        final int msgType = commandDecoder.msgType();
        final String correlationId = commandDecoder.correlationId();
        final String cacheId = commandDecoder.cacheId();
        final String key = commandDecoder.key();

        if (msgType == CacheRequestMessageTypes.CREATE_CACHE_MSG_ID
                || msgType == CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID) {
            enqueueCommandResponse(response, correlationId, cacheId, key, "");
        } else if (msgType == CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID) {
            enqueueCommandResponse(response, correlationId, cacheId, key, key + "-val");
        } else if (msgType == CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID) {
            final Map<String, String> first = new LinkedHashMap<>();
            first.put("a", "1");
            first.put("b", "2");
            enqueueEntries(response, correlationId, cacheId, first, false);
            enqueueEntries(response, correlationId, cacheId, Map.of("c", "3"), true);
        } else if (msgType == CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID) {
            enqueueStats(response, correlationId);
        } else {
            enqueueError(response, correlationId, "Unknown msgType " + msgType);
        }
    }

    private void handleSubscribe(Publication response) {
        final java.util.List<String> cacheIds = new java.util.ArrayList<>();
        final var group = subscribeDecoder.cacheIds();
        while (group.hasNext()) {
            group.next();
            cacheIds.add(group.cacheId());
        }
        final String correlationId = subscribeDecoder.correlationId();
        final String cacheId = cacheIds.isEmpty() ? "" : cacheIds.get(0);
        enqueueStreamUpdate(response, correlationId, cacheId, "k", "v");
    }

    private void enqueueCommandResponse(Publication response, String correlationId, String cacheId,
                                        String key, String value) {
        commandResponseEncoder.wrapAndApplyHeader(scratch, 0, headerEncoder)
                .status(OperationStatus.SUCCESS)
                .correlationId(correlationId)
                .cacheId(cacheId)
                .key(key)
                .value(value);
        enqueue(response, commandResponseEncoder.limit());
    }

    private void enqueueEntries(Publication response, String correlationId, String cacheId,
                                Map<String, String> items, boolean endOfBatch) {
        var enc = entriesEncoder.wrapAndApplyHeader(scratch, 0, headerEncoder)
                .status(OperationStatus.SUCCESS)
                .endOfBatch(endOfBatch ? BooleanType.T : BooleanType.F);
        var itemsEnc = enc.itemsCount(items.size());
        for (Map.Entry<String, String> entry : items.entrySet()) {
            itemsEnc.next().key(entry.getKey()).value(entry.getValue());
        }
        enc.correlationId(correlationId).cacheId(cacheId);
        enqueue(response, entriesEncoder.limit());
    }

    private void enqueueStats(Publication response, String correlationId) {
        var enc = statsEncoder.wrapAndApplyHeader(scratch, 0, headerEncoder)
                .status(OperationStatus.SUCCESS)
                .endOfBatch(BooleanType.T);
        var statsEnc = enc.statsCount(1);
        statsEnc.next()
                .addedCount(7L)
                .removedCount(2L)
                .clearedCount(1L)
                .size(4L)
                .cacheId("cacheStats");
        enc.correlationId(correlationId);
        enqueue(response, statsEncoder.limit());
    }

    private void enqueueStreamUpdate(Publication response, String correlationId, String cacheId,
                                     String key, String value) {
        streamUpdateEncoder.wrapAndApplyHeader(scratch, 0, headerEncoder)
                .eventType(UpdateEventType.ADD_ITEM)
                .cacheId(cacheId)
                .key(key)
                .value(value)
                .correlationId(correlationId);
        enqueue(response, streamUpdateEncoder.limit());
    }

    private void enqueueError(Publication response, String correlationId, String message) {
        errorEncoder.wrapAndApplyHeader(scratch, 0, headerEncoder)
                .status(OperationStatus.ERROR)
                .correlationId(correlationId)
                .message(message);
        enqueue(response, errorEncoder.limit());
    }

    private void enqueue(Publication response, int length) {
        final byte[] bytes = new byte[length];
        scratch.getBytes(0, bytes, 0, length);
        pending.addLast(new PendingFrame(response, new org.agrona.concurrent.UnsafeBuffer(bytes), length));
    }

    private record PendingFrame(Publication publication, DirectBuffer buffer, int length) {
    }
}
