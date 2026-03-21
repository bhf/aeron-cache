package com.bhf.aeroncache.services.cache.snapshot;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Encode and decode the cacheId for snapshotting and loading.
 * @param <I> The type used to identify the cache.
 */
public interface CacheIdSnapshotCodec<I> {

    /**
     * Serialize the cache Id as part of a snapshot.
     * @param cacheId The cache Id.
     * @param buffer The buffer to write too.
     * @param offset The offset at which to write.
     * @return The next offset to write at.
     */
    int serializeCacheId(I cacheId, MutableDirectBuffer buffer, int offset);

    /**
     * Deserialize the cacheId as part of loading a snapshot.
     * @param buffer to read from.
     * @param offset at which to write.
     * @param cacheId to populate.
     * @return The next offset to read from.
     */
    int deserializeCacheId(DirectBuffer buffer, int offset, I cacheId);
}
