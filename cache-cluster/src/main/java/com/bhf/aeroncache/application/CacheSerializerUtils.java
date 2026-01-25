package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cache.CacheEntryCodec;
import com.bhf.aeroncache.services.cache.CacheIdCodec;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Map;

public class CacheSerializerUtils {

    public static CacheEntryCodec<ReusableString, ReusableString> getCacheEntrySerializer() {
        return new CacheEntryCodec<>() {
            @Override
            public int serialize(ReusableString key, ReusableString value, MutableDirectBuffer buffer, int offset) {
                int written = 0;
                written += buffer.putStringAscii(offset + written, key.value());
                written += buffer.putStringAscii(offset + written, value.value());
                return written + offset;
            }

            @Override
            public int deserialize(DirectBuffer buffer, int offset, Map<ReusableString, ReusableString> cache) {
                var key = buffer.getStringAscii(offset);
                offset += 4 + key.length();
                var value = buffer.getStringAscii(offset);
                offset += 4 + value.length();
                var k = new ReusableString();
                k.copyFrom(key);
                var v = new ReusableString();
                v.copyFrom(value);
                cache.put(k, v);
                return offset;
            }
        };
    }

    public static CacheIdCodec<ReusableString> getCacheIdSerializer() {
        return new CacheIdCodec<>() {
            @Override
            public int serializeCacheId(ReusableString cacheId, MutableDirectBuffer buffer, int offset) {
                return buffer.putStringAscii(offset, cacheId.value());
            }

            @Override
            public int getCacheId(DirectBuffer buffer, int offset, ReusableString cacheId) {
                var cid = buffer.getStringAscii(offset);
                cacheId.copyFrom(cid);
                return offset + (cid.length() + 4);
            }
        };
    }
}
