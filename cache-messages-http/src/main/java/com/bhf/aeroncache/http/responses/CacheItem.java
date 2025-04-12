package com.bhf.aeroncache.http.responses;

/**
 * A cache item.
 * @param key The key of the item.
 * @param value The value of the item.
 */
public record CacheItem(String key, String value) {
}
