package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.messages.*;
import org.agrona.MutableDirectBuffer;

/**
 * Encode requests going to Aeron Cache.
 */
public class RegularStringCacheRequestEncoder implements CacheRequestEncoder<String, String, String> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final GetCacheEntryEncoder getCacheEntryEncoder = new GetCacheEntryEncoder();
    private final ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    private final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    private final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    private final GetAllCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCacheEntriesEncoder();
    private final CacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CacheSubscriptionRequestEncoder();
    private final CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CacheUnsubscribeRequestEncoder();
    private final GetCacheStatsEncoder getCacheStatsEncoder = new GetCacheStatsEncoder();

    @Override
    public int encodeCreateCacheRequest(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return createCacheEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntry(String requestId, String cacheId, String key, String value, MutableDirectBuffer msgBuffer) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId)
                .key(key)
                .entryValue(value);
        return addCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntry(String requestId, String cacheId, String key, MutableDirectBuffer msgBuffer) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return getCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeClearCache(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return clearCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return deleteCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, String cacheId, String key, MutableDirectBuffer msgBuffer) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).key(key).requestId(requestId);
        return removeCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntries(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return getAllCacheEntriesEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheSubscribe(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return cacheSubscriptionRequestEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheUnsubscribe(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId).requestId(requestId);
        return cacheUnsubscribeRequestEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer) {
        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        return getCacheStatsEncoder.encodedLength()+ headerEncoder.encodedLength();
    }
}
