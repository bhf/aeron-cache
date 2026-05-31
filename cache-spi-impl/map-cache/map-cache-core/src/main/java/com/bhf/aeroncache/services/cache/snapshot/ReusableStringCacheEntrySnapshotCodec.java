package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.types.ReusableString;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Comparator;
import java.util.Map;

@Log4j2
public class ReusableStringCacheEntrySnapshotCodec implements CacheEntrySnapshotCodec<ReusableString, ReusableString> {
    @Override
    public int serializeCacheEntry(ReusableString key, ReusableString value, MutableDirectBuffer buffer, int offset) {
        int written = 0;
        written += buffer.putStringUtf8(offset + written, key.value());
        written += buffer.putStringUtf8(offset + written, value.value());
        log.debug("Encoding key="+key.value()+", value="+value.value());
        return written + offset;
    }

    @Override
    public int deserializeCacheEntry(DirectBuffer buffer, int offset, Map<ReusableString, ReusableString> cache) {
        int read = 0;
        var key = buffer.getStringUtf8(offset + read);
        read += buffer.getInt(offset + read) + 4;

        var value = buffer.getStringUtf8(offset + read);
        read += buffer.getInt(offset + read) + 4;

        var k = new ReusableString();
        k.copyFrom(key);
        var v = new ReusableString();
        v.copyFrom(value);
        cache.put(k, v);

        log.debug("Decoded key="+k.value()+", value="+v.value());

        return offset+read;
    }

    @Override
    public Comparator<ReusableString> getKeyComparator() {
        return Comparator.comparing(ReusableString::value);
    }
}
