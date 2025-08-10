package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.messages.*;
import org.agrona.MutableDirectBuffer;

/**
 * Encode requests going to Aeron Cache.
 */
public class CacheRequestEncoder {

    public static int encodeCreateCacheRequest(CreateCacheEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeAddCacheEntry(AddCacheEntryEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key, String value) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId)
                .key(key)
                .entryValue(value);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeGetCacheEntry(GetCacheEntryEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeClearCache(ClearCacheEncoder clearCacheEncoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return clearCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    public static int encodeDeleteCache(DeleteCacheEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return encoder.encodedLength()+headerEncoder.encodedLength();
    }

    public static int encodeRemoveCacheEntry(RemoveCacheEntryEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeGetCacheEntries(GetAllCacheEntriesEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeCacheSubscribe(CacheSubscriptionRequestEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public static int encodeCacheUnsubscribe(CacheUnsubscribeRequestEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return encoder.encodedLength()+headerEncoder.encodedLength();
    }

    public static int encodeGetAllCacheStats(GetCacheStatsEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }
}
