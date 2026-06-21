package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.services.integrity.StreamingHasher;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Comparator;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
public class CountersCacheEntrySnapshotCodec implements CacheEntrySnapshotCodec<ReusableString, ReusableLong> {

    private final StreamingHasher<String> hasher;

    @Override
    public int serializeCacheEntry(ReusableString key, ReusableLong value, MutableDirectBuffer buffer, int offset) {
        int written = 0;
        var itemKey = key.value();
        var itemValue = value.value();
        hasher.reset();
        hasher.addToHash(itemKey).addToHash(itemValue);
        long hash = hasher.getHash();
        written += buffer.putStringUtf8(offset + written, itemKey);

        buffer.putLong(offset + written, itemValue);
        written += 8;

        buffer.putLong(offset + written, hash);
        written += 8;

        log.debug("Encoding key=" + key.value() + ", value=" + value.value());
        return written + offset;
    }

    @Override
    public int deserializeCacheEntry(DirectBuffer buffer, int offset, Map<ReusableString, ReusableLong> cache) {
        int read = 0;
        var key = buffer.getStringUtf8(offset + read);
        int keyLength = buffer.getInt(offset + read);
        read += (keyLength == -1 ? 0 : keyLength) + 4;

        var value = buffer.getLong(offset + read);
        read += 8;

        var k = new ReusableString();
        k.copyFrom(key);
        var v = new ReusableLong();
        v.copyFrom(value);
        cache.put(k, v);

        long recordedHash = buffer.getLong(offset + read);
        read += 8;

        hasher.reset();
        hasher.addToHash(key).addToHash(value);
        long recalculatedHash = hasher.getHash();

        log.debug("Decoded key: {}, value: {}, hash: {}, recalculated hash: {}", key, value, recordedHash, recalculatedHash);

        if (recordedHash != recalculatedHash) {
            log.warn("Mismatch between recorded hash: {} and recalculated hash: {}, Decoded key: {}, " +
                    "value: {}, hash: {}, recalculated hash: {}", recordedHash, recalculatedHash,
                    key, value, recordedHash, recalculatedHash);
        }

        return offset + read;
    }

    @Override
    public Comparator<ReusableString> getKeyComparator() {
        return Comparator.comparing(ReusableString::value);
    }
}
