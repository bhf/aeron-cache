package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cache.CacheEntryCodec;
import com.bhf.aeroncache.services.cache.CacheIdCodec;
import com.bhf.aeroncache.types.ReusableString;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.Map;

@Log4j2
public class CacheSerializerUtils {

    public static CacheEntryCodec<ReusableString, ReusableString> getCacheEntrySerializer() {
        return new CacheEntryCodec<>() {
            @Override
            public int serialize(ReusableString key, ReusableString value, MutableDirectBuffer buffer, int offset) {
                int written = 0;
                var keyLength = key.value().length();
                buffer.putInt(offset+written, keyLength);
                written += 4;
                buffer.putStringWithoutLengthAscii(offset + written, key.value());
                written += keyLength;
                log.trace("Writing key length="+keyLength+", key="+key.value());

                var valueLength = value.value().length();
                buffer.putInt(offset+written, valueLength);
                written += 4;
                buffer.putStringWithoutLengthAscii(offset + written, value.value());
                written += valueLength;
                log.trace("Writing value length="+valueLength+", value="+value.value());

                log.debug("Encoding key="+key.value()+", value="+value.value());
                return written + offset;
            }

            @Override
            public int deserialize(DirectBuffer buffer, int offset, Map<ReusableString, ReusableString> cache) {
                int read = 0;
                var keyLength = buffer.getInt(offset + read);
                read += 4;
                var key = buffer.getStringWithoutLengthAscii(offset + read, keyLength);
                read += keyLength;

                var valueLength = buffer.getInt(offset+read);
                read+=4;
                var value = buffer.getStringWithoutLengthAscii(offset+read, valueLength);
                read += valueLength;
                var k = new ReusableString();
                k.copyFrom(key);
                var v = new ReusableString();
                v.copyFrom(value);
                cache.put(k, v);

                log.debug("Decoded key="+k.value()+", value="+v.value());

                return offset+read;
            }
        };
    }

    public static CacheIdCodec<ReusableString> getCacheIdSerializer() {
        return new CacheIdCodec<>() {
            @Override
            public int serializeCacheId(ReusableString cacheId, MutableDirectBuffer buffer, int offset) {
                var value = cacheId.value();
                var length = value.length();
                buffer.putInt(offset, length);
                buffer.putStringWithoutLengthAscii(offset+4, value);
                return offset + 4 + length;
            }

            @Override
            public int getCacheId(DirectBuffer buffer, int offset, ReusableString cacheId) {
                var length = buffer.getInt(offset);
                var value = buffer.getStringWithoutLengthAscii(offset+4, length);
                log.trace("Cache ID legnth="+length+", value="+value);
                cacheId.copyFrom(value);
                return offset + 4 + length;
            }
        };
    }
}
