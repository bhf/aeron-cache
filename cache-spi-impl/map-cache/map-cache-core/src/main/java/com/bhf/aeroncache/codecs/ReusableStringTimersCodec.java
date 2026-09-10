package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.models.PendingRemove;
import com.bhf.aeroncache.services.integrity.MultiTypeStreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import io.aeron.cluster.service.Cluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;

@RequiredArgsConstructor
@Log4j2
public class ReusableStringTimersCodec implements CacheTimersCodec<ReusableString, ReusableString> {

    private final MultiTypeStreamingHasher<String, Long> hasher;

    @Override
    public int encodeCacheTimer(MutableDirectBuffer timersBuffer, int offset, long timerCorrelationId, ReusableString key, ReusableString cacheId) {
        int startOffset = offset;

        timersBuffer.putLong(offset, timerCorrelationId);
        offset += 8;

        var itemKey = key.value();
        offset += timersBuffer.putStringUtf8(offset, itemKey);
        var timerCacheId = cacheId.value();
        offset += timersBuffer.putStringUtf8(offset, timerCacheId);

        hasher.reset();
        hasher.addFirstTypeToHash(itemKey).addFirstTypeToHash(timerCacheId).addSecondTypeToHash(timerCorrelationId);
        long hash = hasher.getHash();

        timersBuffer.putLong(offset, hash);
        offset += 8;

        return offset - startOffset;
    }

    @Override
    public void decodeCacheTimers(int timersSize, Long2ObjectHashMap<PendingRemove<ReusableString, ReusableString>> pendingRemoves, DirectBuffer buffer, int offset, Cluster cluster) {
        for (int i = 0; i < timersSize; i++) {
            long timerCorrelationId = buffer.getLong(offset);
            offset += 8;

            var keyLength = buffer.getInt(offset);
            var keyStr = buffer.getStringUtf8(offset);
            offset += 4 + keyLength;

            var cacheIdLength = buffer.getInt(offset);
            var cacheIdStr = buffer.getStringUtf8(offset);
            offset += 4 + cacheIdLength;

            long recordedHash = buffer.getLong(offset);
            offset += 8;

            ReusableString key = new ReusableString();
            key.copyFrom(keyStr);

            ReusableString cacheId = new ReusableString();
            cacheId.copyFrom(cacheIdStr);

            hasher.reset();
            hasher.addFirstTypeToHash(keyStr).addFirstTypeToHash(cacheIdStr).addSecondTypeToHash(timerCorrelationId);
            var recalculatedHash = hasher.getHash();

            if (recordedHash != recalculatedHash) {
                log.warn("Recorded hash {} not the same as calculated hash {}, cache {}, " +
                        "key {}, timer ID {}", recordedHash, recalculatedHash, cacheIdStr, keyStr, timerCorrelationId);
            }

            PendingRemove<ReusableString, ReusableString> pendingRemove = new PendingRemove<>(timerCorrelationId, cacheId, key);

            pendingRemoves.put(timerCorrelationId, pendingRemove);

            if (i % 100 == 0) {
                cluster.idleStrategy().idle();
            }
        }
    }
}
