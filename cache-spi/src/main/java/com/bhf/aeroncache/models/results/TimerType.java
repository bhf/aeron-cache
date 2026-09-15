package com.bhf.aeroncache.models.results;

/**
 * Discriminates whether a pending TTL removal timer belongs to a regular cache
 * or a counter cache.
 */
public enum TimerType {
    CACHE,
    COUNTER
}
