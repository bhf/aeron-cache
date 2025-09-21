package com.bhf.aeroncache.services.cache;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Map;

/**
 * Encode and decode cache entries to and from a snapshot.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public interface CacheEntryCodec<K, V> {

    /**
     * Serialize the key and value to a {@link DirectBuffer}.
     *
     * @param key    The key to be serialized.
     * @param value  The value to be serialized.
     * @param buffer The buffer to write too.
     * @param offset The starting offset to write at.
     * @return The next offset to write at.
     */
    int serialize(K key, V value, MutableDirectBuffer buffer, int offset);

    /**
     * Deserialize key value pairs into the provided Map.
     *
     * @param buffer to deserialize from.
     * @param offset at which to start reading.
     * @param cache  to be populated.
     * @return The next offset to read from.
     */
    int deserialize(DirectBuffer buffer, int offset, Map<K, V> cache);
}
