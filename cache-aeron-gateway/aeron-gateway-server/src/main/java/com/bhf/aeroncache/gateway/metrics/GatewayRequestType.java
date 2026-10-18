package com.bhf.aeroncache.gateway.metrics;

/**
 * The kinds of request serviced by the Aeron gateway, used to keep a separate count of each.
 */
public enum GatewayRequestType {
    CREATE_CACHE("CreateCache"),
    ADD_CACHE_ENTRY("AddCacheEntry"),
    PATCH_CACHE_ENTRY("PatchCacheEntry"),
    GET_CACHE_ENTRY("GetCacheEntry"),
    CLEAR_CACHE("ClearCache"),
    DELETE_CACHE("DeleteCache"),
    REMOVE_CACHE_ENTRY("RemoveCacheEntry"),
    CANCEL_CACHE_ITEM_REMOVAL("CancelCacheItemRemoval"),
    GET_CACHE_ENTRIES("GetCacheEntries"),
    GET_CACHE_STATS("GetCacheStats"),
    GET_TIMERS("GetTimers"),
    CREATE_COUNTER_CACHE("CreateCounterCache"),
    ADD_COUNTER_ENTRY("AddCounterEntry"),
    GET_COUNTER_ENTRY("GetCounterEntry"),
    CLEAR_COUNTER_CACHE("ClearCounterCache"),
    DELETE_COUNTER_CACHE("DeleteCounterCache"),
    GET_COUNTER_ENTRIES("GetCounterEntries"),
    GET_COUNTER_STATS("GetCounterStats"),
    REMOVE_COUNTER_ENTRY("RemoveCounterEntry"),
    CANCEL_COUNTER_ITEM_REMOVAL("CancelCounterItemRemoval"),
    INCREMENT_COUNTER_ENTRY("IncrementCounterEntry"),
    DECREMENT_COUNTER_ENTRY("DecrementCounterEntry"),
    SET_COUNTER_ENTRY("SetCounterEntry"),
    SUBSCRIBE("Subscribe"),
    UNSUBSCRIBE("Unsubscribe"),
    BULK_OPS("BulkOps"),
    UNKNOWN("Unknown");

    private final String label;

    GatewayRequestType(String label) {
        this.label = label;
    }

    /**
     * @return a short, human readable name suitable for use in a metric label.
     */
    public String label() {
        return label;
    }
}
