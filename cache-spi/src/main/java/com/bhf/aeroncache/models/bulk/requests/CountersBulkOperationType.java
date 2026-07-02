package com.bhf.aeroncache.models.bulk.requests;

public enum CountersBulkOperationType {
    NONE,
    CREATE_COUNTER_CACHE,
    ADD_COUNTER,
    REMOVE_COUNTER,
    CLEAR_COUNTER_CACHE,
    GET_COUNTER,
    DELETE_COUNTER_CACHE,
    INCREMENT_COUNTER_CACHE,
    DECREMENT_COUNTER_CACHE,
    SET_COUNTER_CACHE
}
