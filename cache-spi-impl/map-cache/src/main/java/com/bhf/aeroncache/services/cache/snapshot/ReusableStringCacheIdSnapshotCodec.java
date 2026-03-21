package com.bhf.aeroncache.services.cache.snapshot;

import com.bhf.aeroncache.types.ReusableString;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

@Log4j2
public class ReusableStringCacheIdSnapshotCodec implements CacheIdSnapshotCodec<ReusableString> {

    @Override
    public int serializeCacheId(ReusableString cacheId, MutableDirectBuffer buffer, int offset) {
        var value = cacheId.value();
        var length = value.length();
        buffer.putInt(offset, length);
        buffer.putStringWithoutLengthAscii(offset+4, value);
        return offset + 4 + length;
    }

    @Override
    public int deserializeCacheId(DirectBuffer buffer, int offset, ReusableString cacheId) {
        var length = buffer.getInt(offset);
        var value = buffer.getStringWithoutLengthAscii(offset+4, length);
        log.trace("Cache ID legnth="+length+", value="+value);
        cacheId.copyFrom(value);
        return offset + 4 + length;
    }
}
