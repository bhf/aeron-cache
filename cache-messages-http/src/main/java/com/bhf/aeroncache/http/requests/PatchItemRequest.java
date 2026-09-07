package com.bhf.aeroncache.http.requests;

/**
 * A request to patch (deep-merge) an item in the cache.
 *
 * @param value The patch to merge into the existing value.
 */
public record PatchItemRequest(String value) {
}
