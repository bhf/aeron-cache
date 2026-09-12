package com.bhf.aeroncache.ws.bidi.messages;

import com.bhf.aeroncache.models.CacheRequestMessageTypes;

/**
 * The command operations exposed over the bidirectional websocket protocol.
 *
 * <p>This is the JSON-facing vocabulary for {@link BidiCommand#op()}, mirroring the command surface the
 * Aeron gateway exposes via {@code msgType}. Each value maps onto a {@link CacheRequestMessageTypes} id so
 * the websocket dispatch aligns with the gateway and the underlying cluster request protocol. Counter
 * operations use ids {@code >= 100} (see {@link #counters()}).</p>
 *
 * <p>Subscribe/unsubscribe are deliberately not commands here: they are their own inbound frames
 * ({@link BidiSubscribe}/{@link BidiUnsubscribe}), matching the gateway. Bulk operations are likewise
 * not exposed, matching the gateway.</p>
 */
public enum WsOp {

    CREATE_CACHE(CacheRequestMessageTypes.CREATE_CACHE_MSG_ID),
    ADD_CACHE_ENTRY(CacheRequestMessageTypes.ADD_CACHE_ENTRY_MSG_ID),
    PATCH_CACHE_ENTRY(CacheRequestMessageTypes.PATCH_CACHE_ENTRY_MSG_ID),
    GET_CACHE_ENTRY(CacheRequestMessageTypes.GET_CACHE_ENTRY_MSG_ID),
    CLEAR_CACHE(CacheRequestMessageTypes.CLEAR_CACHE_MSG_ID),
    DELETE_CACHE(CacheRequestMessageTypes.DELETE_CACHE_MSG_ID),
    REMOVE_CACHE_ENTRY(CacheRequestMessageTypes.REMOVE_CACHE_ENTRY_MSG_ID),
    CANCEL_CACHE_ITEM_REMOVAL(CacheRequestMessageTypes.CANCEL_CACHE_ITEM_REMOVAL_MSG_ID),
    GET_CACHE_ENTRIES(CacheRequestMessageTypes.GET_CACHE_ENTRIES_MSG_ID),
    GET_CACHE_STATS(CacheRequestMessageTypes.GET_CACHE_STATS_MSG_ID),

    CREATE_COUNTER_CACHE(CacheRequestMessageTypes.CREATE_COUNTER_CACHE_MSG_ID),
    ADD_COUNTER_ENTRY(CacheRequestMessageTypes.ADD_COUNTER_ENTRY_MSG_ID),
    GET_COUNTER_ENTRY(CacheRequestMessageTypes.GET_COUNTER_ENTRY_MSG_ID),
    CLEAR_COUNTER_CACHE(CacheRequestMessageTypes.CLEAR_COUNTER_CACHE_MSG_ID),
    DELETE_COUNTER_CACHE(CacheRequestMessageTypes.DELETE_COUNTER_CACHE_MSG_ID),
    REMOVE_COUNTER_ENTRY(CacheRequestMessageTypes.REMOVE_COUNTER_ENTRY_MSG_ID),
    CANCEL_COUNTER_ITEM_REMOVAL(CacheRequestMessageTypes.CANCEL_COUNTER_ITEM_REMOVAL_MSG_ID),
    GET_COUNTER_ENTRIES(CacheRequestMessageTypes.GET_COUNTER_ENTRIES_MSG_ID),
    GET_COUNTER_STATS(CacheRequestMessageTypes.GET_COUNTER_STATS_MSG_ID),
    INCREMENT_COUNTER_ENTRY(CacheRequestMessageTypes.INCREMENT_COUNTER_ENTRY_MSG_ID),
    DECREMENT_COUNTER_ENTRY(CacheRequestMessageTypes.DECREMENT_COUNTER_ENTRY_MSG_ID),
    SET_COUNTER_ENTRY(CacheRequestMessageTypes.SET_COUNTER_ENTRY_MSG_ID);

    private final int msgType;

    WsOp(int msgType) {
        this.msgType = msgType;
    }

    /**
     * The {@link CacheRequestMessageTypes} id this operation maps onto.
     *
     * @return the underlying cache request message type id.
     */
    public int msgType() {
        return msgType;
    }

    /**
     * Whether this operation targets the counters cache flavour (as opposed to regular caches).
     *
     * @return {@code true} for counter operations.
     */
    public boolean counters() {
        return msgType >= CacheRequestMessageTypes.CREATE_COUNTER_CACHE_MSG_ID;
    }
}
