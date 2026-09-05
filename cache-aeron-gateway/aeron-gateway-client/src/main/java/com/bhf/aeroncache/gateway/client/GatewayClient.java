package com.bhf.aeroncache.gateway.client;

import com.bhf.aeroncache.gateway.messages.GatewayCommandResponseDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayEntriesDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayErrorDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayStatsDecoder;
import com.bhf.aeroncache.gateway.messages.GatewayStreamUpdateDecoder;
import com.bhf.aeroncache.gateway.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.models.CacheRequestMessageTypes;
import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import lombok.extern.log4j.Log4j2;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client for the Aeron Cache gateway, the counterpart to the gateway server's ingress agent.
 * <p>
 * Modeled on {@link io.aeron.response.ResponseClient}: it opens a response subscription
 * ({@code control-mode=response}) to receive responses/updates and an
 * {@link ExclusivePublication} carrying {@code response-correlation-id} so the server can route
 * responses back without an application level handshake. Requests are encoded with the shared
 * {@code gateway-schema.xml} contract.
 * <p>
 * Drive {@link #doWork()} from a single agent thread (e.g. an {@link org.agrona.concurrent.AgentRunner}).
 * Command send methods are safe to call from other threads; they are serialised internally and the
 * request {@link ExclusivePublication} is only ever offered to under that lock. All listener callbacks
 * fire on the agent thread.
 */
@Log4j2
public class GatewayClient implements Agent, AutoCloseable {

    private static final int FRAGMENT_LIMIT = 10;
    private static final int MAX_OFFER_ATTEMPTS = 1000;

    private final Aeron aeron;
    private final int requestStreamId;
    private final int responseStreamId;
    private final ChannelUriStringBuilder requestUriBuilder;
    private final ChannelUriStringBuilder responseUriBuilder;

    private final List<GatewayClientListener> listeners = new CopyOnWriteArrayList<>();

    private final Object sendLock = new Object();
    private final MutableDirectBuffer sendBuffer = new ExpandableArrayBuffer(4096);
    private final GatewayRequestWriter requestWriter = new GatewayRequestWriter();

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final GatewayCommandResponseDecoder commandResponseDecoder = new GatewayCommandResponseDecoder();
    private final GatewayEntriesDecoder entriesDecoder = new GatewayEntriesDecoder();
    private final GatewayStatsDecoder statsDecoder = new GatewayStatsDecoder();
    private final GatewayStreamUpdateDecoder streamUpdateDecoder = new GatewayStreamUpdateDecoder();
    private final GatewayErrorDecoder errorDecoder = new GatewayErrorDecoder();
    private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);

    private volatile ExclusivePublication publication;
    private Subscription subscription;

    /**
     * @param aeron             the Aeron client connected to the same media driver.
     * @param requestEndpoint   the gateway server's request endpoint (host:port).
     * @param requestStreamId   the gateway request stream id.
     * @param responseControl   the gateway server's response control endpoint (host:port).
     * @param responseStreamId  the gateway response stream id.
     * @param listener          the initial listener (may be {@code null}); more can be added later.
     */
    public GatewayClient(Aeron aeron,
                         String requestEndpoint,
                         int requestStreamId,
                         String responseControl,
                         int responseStreamId,
                         GatewayClientListener listener) {
        this.aeron = aeron;
        this.requestStreamId = requestStreamId;
        this.responseStreamId = responseStreamId;
        this.requestUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .endpoint(requestEndpoint);
        this.responseUriBuilder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode("response")
                .controlEndpoint(responseControl);
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Register an additional listener for responses and updates.
     */
    public void addListener(GatewayClientListener listener) {
        listeners.add(listener);
    }

    // ------------------------------------------------------------------ Agent

    @Override
    public int doWork() {
        int work = 0;

        if (subscription == null) {
            subscription = aeron.addSubscription(responseUriBuilder.build(), responseStreamId);
            work++;
        }

        if (publication == null) {
            publication = aeron.addExclusivePublication(
                    requestUriBuilder.responseCorrelationId(subscription.registrationId()).build(),
                    requestStreamId);
            work++;
        }

        work += subscription.poll(fragmentAssembler, FRAGMENT_LIMIT);
        return work;
    }

    @Override
    public String roleName() {
        return "AeronCache-Gateway-Client";
    }

    @Override
    public void onClose() {
        close();
    }

    @Override
    public void close() {
        final ExclusivePublication pub = publication;
        if (pub != null) {
            pub.revokeOnClose();
        }
        CloseHelper.quietCloseAll(pub, subscription);
        publication = null;
        subscription = null;
    }

    /**
     * @return {@code true} once both the response subscription and request publication are connected.
     */
    public boolean isConnected() {
        final ExclusivePublication pub = publication;
        return subscription != null && subscription.isConnected() && pub != null && pub.isConnected();
    }

    // ------------------------------------------------------------------ cache commands

    public long createCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.CREATE_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long addEntry(String correlationId, String cacheId, String key, String value, long ttl) {
        return sendCommand(CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID, ttl, 0L, correlationId, cacheId, key, value);
    }

    public long getEntry(String correlationId, String cacheId, String key) {
        return sendCommand(CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID, 0L, 0L, correlationId, cacheId, key, null);
    }

    public long getEntries(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long clearCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.CLEAR_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long deleteCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.DELETE_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long removeEntry(String correlationId, String cacheId, String key) {
        return sendCommand(CacheRequestMessageTypes.REMOVE_CACHE_ENTRY_MSG_ID, 0L, 0L, correlationId, cacheId, key, null);
    }

    public long getStats(String correlationId) {
        return sendCommand(CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID, 0L, 0L, correlationId, null, null, null);
    }

    // ------------------------------------------------------------------ counter commands

    public long createCounterCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.CREATE_COUNTER_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long addCounterEntry(String correlationId, String cacheId, String key, long counterValue, long ttl) {
        return sendCommand(CacheRequestMessageTypes.ADD_COUNTER_ENTRY_MSG_ID, ttl, counterValue, correlationId, cacheId, key, null);
    }

    public long getCounterEntry(String correlationId, String cacheId, String key) {
        return sendCommand(CacheRequestMessageTypes.GET_COUNTER_ENTRY_MSG_ID, 0L, 0L, correlationId, cacheId, key, null);
    }

    public long getCounterEntries(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.GET_COUNTER_ENTRIES_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long clearCounterCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.CLEAR_COUNTER_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long deleteCounterCache(String correlationId, String cacheId) {
        return sendCommand(CacheRequestMessageTypes.DELETE_COUNTER_CACHE_MSG_ID, 0L, 0L, correlationId, cacheId, null, null);
    }

    public long removeCounterEntry(String correlationId, String cacheId, String key) {
        return sendCommand(CacheRequestMessageTypes.REMOVE_COUNTER_ENTRY_MSG_ID, 0L, 0L, correlationId, cacheId, key, null);
    }

    public long getCounterStats(String correlationId) {
        return sendCommand(CacheRequestMessageTypes.GET_COUNTER_STATS_MSG_ID, 0L, 0L, correlationId, null, null, null);
    }

    public long incrementCounter(String correlationId, String cacheId, String key, long delta, long ttl) {
        return sendCommand(CacheRequestMessageTypes.INCREMENT_COUNTER_ENTRY_MSG_ID, ttl, delta, correlationId, cacheId, key, null);
    }

    public long decrementCounter(String correlationId, String cacheId, String key, long delta, long ttl) {
        return sendCommand(CacheRequestMessageTypes.DECREMENT_COUNTER_ENTRY_MSG_ID, ttl, delta, correlationId, cacheId, key, null);
    }

    public long setCounter(String correlationId, String cacheId, String key, long counterValue, long ttl) {
        return sendCommand(CacheRequestMessageTypes.SET_COUNTER_ENTRY_MSG_ID, ttl, counterValue, correlationId, cacheId, key, null);
    }

    // ------------------------------------------------------------------ subscriptions

    /**
     * Subscribe to streaming updates for the given caches.
     *
     * @param counters {@code true} to subscribe to counter caches, {@code false} for regular caches.
     */
    public long subscribe(String correlationId, List<String> cacheIds, boolean sendSnapshot, boolean counters) {
        synchronized (sendLock) {
            final int length = requestWriter.encodeSubscribe(sendBuffer, correlationId, cacheIds, sendSnapshot, counters);
            return offer(length);
        }
    }

    /**
     * Unsubscribe from streaming updates for a cache.
     *
     * @param counters {@code true} to unsubscribe from a counter cache, {@code false} for a regular cache.
     */
    public long unsubscribe(String correlationId, String cacheId, boolean counters) {
        synchronized (sendLock) {
            final int length = requestWriter.encodeUnsubscribe(sendBuffer, correlationId, cacheId, counters);
            return offer(length);
        }
    }

    // ------------------------------------------------------------------ internals

    private long sendCommand(int msgType, long ttl, long counterValue,
                             String correlationId, String cacheId, String key, String value) {
        synchronized (sendLock) {
            final int length = requestWriter.encodeCommand(sendBuffer, msgType, ttl, counterValue, correlationId, cacheId, key, value);
            return offer(length);
        }
    }

    private long offer(int length) {
        final ExclusivePublication pub = publication;
        if (pub == null) {
            return Aeron.NULL_VALUE;
        }
        long result = 0;
        for (int attempt = 0; attempt < MAX_OFFER_ATTEMPTS; attempt++) {
            result = pub.offer(sendBuffer, 0, length);
            if (result > 0) {
                return result;
            }
            if (result == ExclusivePublication.CLOSED
                    || result == ExclusivePublication.NOT_CONNECTED
                    || result == ExclusivePublication.MAX_POSITION_EXCEEDED) {
                return result;
            }
        }
        log.warn("Gave up offering request frame of length {} after {} attempts", length, MAX_OFFER_ATTEMPTS);
        return result;
    }

    private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();
        final int blockLength = headerDecoder.blockLength();
        final int version = headerDecoder.version();
        final int bodyOffset = offset + headerDecoder.encodedLength();

        if (templateId == GatewayCommandResponseDecoder.TEMPLATE_ID) {
            decodeCommandResponse(buffer, bodyOffset, blockLength, version);
        } else if (templateId == GatewayEntriesDecoder.TEMPLATE_ID) {
            decodeEntries(buffer, bodyOffset, blockLength, version);
        } else if (templateId == GatewayStatsDecoder.TEMPLATE_ID) {
            decodeStats(buffer, bodyOffset, blockLength, version);
        } else if (templateId == GatewayStreamUpdateDecoder.TEMPLATE_ID) {
            decodeStreamUpdate(buffer, bodyOffset, blockLength, version);
        } else if (templateId == GatewayErrorDecoder.TEMPLATE_ID) {
            decodeError(buffer, bodyOffset, blockLength, version);
        } else {
            log.warn("Unknown gateway response templateId {}", templateId);
        }
    }

    private void decodeCommandResponse(DirectBuffer buffer, int offset, int blockLength, int version) {
        commandResponseDecoder.wrap(buffer, offset, blockLength, version);
        final var status = commandResponseDecoder.status();
        final String correlationId = commandResponseDecoder.correlationId();
        final String cacheId = commandResponseDecoder.cacheId();
        final String key = commandResponseDecoder.key();
        final String value = commandResponseDecoder.value();
        for (GatewayClientListener listener : listeners) {
            listener.onCommandResponse(correlationId, status, cacheId, key, value);
        }
    }

    private void decodeEntries(DirectBuffer buffer, int offset, int blockLength, int version) {
        entriesDecoder.wrap(buffer, offset, blockLength, version);
        final var status = entriesDecoder.status();
        final boolean endOfBatch = entriesDecoder.endOfBatch() == com.bhf.aeroncache.gateway.messages.BooleanType.T;
        final Map<String, String> items = new HashMap<>();
        for (GatewayEntriesDecoder.ItemsDecoder item : entriesDecoder.items()) {
            final String key = item.key();
            final String value = item.value();
            items.put(key, value);
        }
        final String correlationId = entriesDecoder.correlationId();
        final String cacheId = entriesDecoder.cacheId();
        for (GatewayClientListener listener : listeners) {
            listener.onEntries(correlationId, status, cacheId, items, endOfBatch);
        }
    }

    private void decodeStats(DirectBuffer buffer, int offset, int blockLength, int version) {
        statsDecoder.wrap(buffer, offset, blockLength, version);
        final var status = statsDecoder.status();
        final boolean endOfBatch = statsDecoder.endOfBatch() == com.bhf.aeroncache.gateway.messages.BooleanType.T;
        final List<GatewayStat> stats = new java.util.ArrayList<>();
        for (GatewayStatsDecoder.StatsDecoder stat : statsDecoder.stats()) {
            final long added = stat.addedCount();
            final long removed = stat.removedCount();
            final long cleared = stat.clearedCount();
            final long size = stat.size();
            final String cacheId = stat.cacheId();
            stats.add(new GatewayStat(cacheId, added, removed, cleared, size));
        }
        final String correlationId = statsDecoder.correlationId();
        for (GatewayClientListener listener : listeners) {
            listener.onStats(correlationId, status, stats, endOfBatch);
        }
    }

    private void decodeStreamUpdate(DirectBuffer buffer, int offset, int blockLength, int version) {
        streamUpdateDecoder.wrap(buffer, offset, blockLength, version);
        final var eventType = streamUpdateDecoder.eventType();
        final String cacheId = streamUpdateDecoder.cacheId();
        final String key = streamUpdateDecoder.key();
        final String value = streamUpdateDecoder.value();
        final String correlationId = streamUpdateDecoder.correlationId();
        for (GatewayClientListener listener : listeners) {
            listener.onStreamUpdate(correlationId, eventType, cacheId, key, value);
        }
    }

    private void decodeError(DirectBuffer buffer, int offset, int blockLength, int version) {
        errorDecoder.wrap(buffer, offset, blockLength, version);
        final var status = errorDecoder.status();
        final String correlationId = errorDecoder.correlationId();
        final String message = errorDecoder.message();
        for (GatewayClientListener listener : listeners) {
            listener.onError(correlationId, status, message);
        }
    }
}
