package com.bhf.aeroncache.gateway.client;

/**
 * A single cache stats record delivered to a {@link GatewayClientListener}.
 *
 * @param cacheId      the cache the stats belong to.
 * @param addedCount   number of entries added over the cache's lifetime.
 * @param removedCount number of entries removed over the cache's lifetime.
 * @param clearedCount number of clear operations over the cache's lifetime.
 * @param size         current number of entries in the cache.
 */
public record GatewayStat(String cacheId, long addedCount, long removedCount, long clearedCount, long size) {
}
