package com.bhf.aeroncache.codecs;

import com.bhf.aeroncache.messages.*;
import org.agrona.MutableDirectBuffer;

/**
 * Encode requests going to Aeron Cache.
 */
public class CacheRequestEncoder {

    MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    GetCacheEntryEncoder getCacheEntryEncoder = new GetCacheEntryEncoder();
    ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    GetAllCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCacheEntriesEncoder();
    CacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CacheSubscriptionRequestEncoder();
    CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CacheUnsubscribeRequestEncoder();
    GetCacheStatsEncoder getCacheStatsEncoder = new GetCacheStatsEncoder();

    public int encodeCreateCacheRequest(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return createCacheEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeAddCacheEntry(MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key, String value) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId)
                .key(key)
                .entryValue(value);
        return addCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeGetCacheEntry(MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return getCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeClearCache(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return clearCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    public int encodeDeleteCache(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return deleteCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    public int encodeRemoveCacheEntry(MutableDirectBuffer msgBuffer, String requestId, String cacheId, String key) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return removeCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeGetCacheEntries(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return getAllCacheEntriesEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeCacheSubscribe(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return cacheSubscriptionRequestEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    public int encodeCacheUnsubscribe(MutableDirectBuffer msgBuffer, String requestId, String cacheId) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return cacheUnsubscribeRequestEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    public int encodeGetAllCacheStats(MutableDirectBuffer msgBuffer, String requestId) {
        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        return getCacheStatsEncoder.encodedLength()+ headerEncoder.encodedLength();
    }
}
