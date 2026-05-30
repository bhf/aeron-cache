package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.models.PendingRemove;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2ObjectHashMap;

public class ReusableStringTimersCodec implements CacheTimersCodec<ReusableString, ReusableString> {

    @Override
    public int encodeCacheTimer(MutableDirectBuffer timersBuffer, int offset, long timerCorrelationId, ReusableString key, ReusableString cacheId) {
        int startOffset = offset;

        timersBuffer.putLong(offset, timerCorrelationId);
        offset += 8;

        offset += timersBuffer.putStringUtf8(offset, key.value());
        offset += timersBuffer.putStringUtf8(offset, cacheId.value());

        return offset - startOffset;
    }

    @Override
    public void decodeCacheTimers(int timersSize, Long2ObjectHashMap<PendingRemove<ReusableString, ReusableString>> pendingRemoves, DirectBuffer buffer, int offset) {
        for (int i = 0; i < timersSize; i++) {
            long timerCorrelationId = buffer.getLong(offset);
            offset += 8;

            int keyLength = buffer.getInt(offset);
            String keyStr = buffer.getStringUtf8(offset);
            offset += 4 + keyLength;

            int cacheIdLength = buffer.getInt(offset);
            String cacheIdStr = buffer.getStringUtf8(offset);
            offset += 4 + cacheIdLength;

            ReusableString key = new ReusableString();
            key.copyFrom(keyStr);

            ReusableString cacheId = new ReusableString();
            cacheId.copyFrom(cacheIdStr);

            PendingRemove<ReusableString, ReusableString> pendingRemove = new PendingRemove<>(timerCorrelationId, cacheId, key);

            pendingRemoves.put(timerCorrelationId, pendingRemove);
        }
    }
}
