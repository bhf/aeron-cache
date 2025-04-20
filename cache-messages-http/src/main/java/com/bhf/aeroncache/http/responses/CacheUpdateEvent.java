package com.bhf.aeroncache.http.responses;

public record CacheUpdateEvent(String cacheId, EventType eventType, String key, String value) {

    public enum EventType{
        ADD_ITEM, REMOVE_ITEM, CLEAR_CACHE, DELETE_CACHE
    }
}
