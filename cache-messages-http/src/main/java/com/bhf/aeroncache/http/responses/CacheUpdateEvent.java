package com.bhf.aeroncache.http.responses;

    public record CacheUpdateEvent<VT>(String cacheId, EventType eventType, String itemKey, VT itemValue, String requestId) {

    public enum EventType{
        ADD_ITEM, REMOVE_ITEM, CLEAR_CACHE, DELETE_CACHE
    }
}
