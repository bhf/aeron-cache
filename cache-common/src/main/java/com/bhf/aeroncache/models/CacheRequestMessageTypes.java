package com.bhf.aeroncache.models;

/**
 * Message types for the {@link org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer}
 * that is processed by the {@link com.bhf.aeroncache.services.cache.CacheClientAgent} or
 * another {@link org.agrona.concurrent.Agent}.
 */
public class CacheRequestMessageTypes {

    public static final int HEARTBEAT = 0;
    public static final int CREATE_CACHE_MSG_ID = 1;
    public static final int ADD_CACHE_ENTRY_MSG_ID = 2;
    public static final int GET_CACHE_ENTRY_MSG_ID = 3;
    public static final int CLEAR_CACHE_MSG_ID = 4;
    public static final int DELETE_CACHE_MSG_ID = 5;
    public static final int GET_CACHE_ENTRIES_MSG_ID = 6;
    public static final int SUBSCRIBE_TO_CACHE_MSG_ID = 7;
    public static final int UNSUBSCRIBE_TO_CACHE_MSG_ID = 8;
    public static final int GET_CACHE_STATS_MSG_ID = 9;
    public static final int REMOVE_CACHE_ENTRY_MSG_ID = 10;
    public static final int BULK_OPS_MSG_ID = 11;

    public static final int CREATE_COUNTER_CACHE_MSG_ID = 101;
    public static final int ADD_COUNTER_ENTRY_MSG_ID = 102;
    public static final int GET_COUNTER_ENTRY_MSG_ID = 103;
    public static final int CLEAR_COUNTER_CACHE_MSG_ID = 104;
    public static final int DELETE_COUNTER_CACHE_MSG_ID = 105;
    public static final int GET_COUNTER_ENTRIES_MSG_ID = 106;
    public static final int SUBSCRIBE_TO_COUNTER_CACHE_MSG_ID = 107;
    public static final int UNSUBSCRIBE_TO_COUNTER_CACHE_MSG_ID = 108;
    public static final int GET_COUNTER_STATS_MSG_ID = 109;
    public static final int REMOVE_COUNTER_ENTRY_MSG_ID = 110;
    public static final int INCREMENT_COUNTER_ENTRY_MSG_ID = 111;
}
