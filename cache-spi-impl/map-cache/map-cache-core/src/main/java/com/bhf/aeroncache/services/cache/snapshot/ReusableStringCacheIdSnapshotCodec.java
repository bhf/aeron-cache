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
        return offset + buffer.putStringUtf8(offset, value);
    }

    @Override
    public int deserializeCacheId(DirectBuffer buffer, int offset, ReusableString cacheId) {
        var value = buffer.getStringUtf8(offset);
        var length = buffer.getInt(offset);
        log.trace("Cache ID value="+value);
        cacheId.copyFrom(value);
        return offset + 4 + length;
    }
}
