package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.services.integrity.StreamingHasher;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

@Log4j2
@RequiredArgsConstructor
public class ReusableStringCacheIdSnapshotCodec implements CacheIdSnapshotCodec<ReusableString> {

    private final StreamingHasher<String> hasher;

    @Override
    public int serializeCacheId(ReusableString cacheId, MutableDirectBuffer buffer, int offset) {
        var value = cacheId.value();
        hasher.reset();
        hasher.addToHash(value);
        long hash = hasher.getHash();
        int bytesWritten = buffer.putStringUtf8(offset, value);
        buffer.putLong(offset + bytesWritten, hash);
        return offset + bytesWritten + 8;
    }

    @Override
    public int deserializeCacheId(DirectBuffer buffer, int offset, ReusableString cacheId) {
        var value = buffer.getStringUtf8(offset);
        var length = buffer.getInt(offset);
        int stringBytes = 4 + (length == -1 ? 0 : length);
        log.trace("Cache ID value=" + value);
        cacheId.clear();

        if (value != null) {
            cacheId.copyFrom(value);
        }

        long recordedHash = buffer.getLong(offset + stringBytes);
        hasher.reset();
        hasher.addToHash(value);
        long recalculatedHash = hasher.getHash();

        if (recordedHash != recalculatedHash) {
            log.warn("Mismatch between recorded hash: {} and recalculated hash: {} " +
                    "for cacheId {}", recordedHash, recalculatedHash, value);
        }

        return offset + stringBytes + 8;
    }
}
