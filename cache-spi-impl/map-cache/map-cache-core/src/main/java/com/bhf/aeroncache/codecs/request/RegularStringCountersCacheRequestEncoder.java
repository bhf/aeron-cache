package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class RegularStringCountersCacheRequestEncoder implements CountersCacheRequestEncoder<String, String, Long> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();    private final CreateCounterCacheEncoder createCacheEncoder = new CreateCounterCacheEncoder();
    private final AddCounterRequestEncoder addCacheEntryEncoder = new AddCounterRequestEncoder();
    private final GetCounterCacheEntryEncoder getCacheEntryEncoder = new GetCounterCacheEntryEncoder();
    private final ClearCounterCacheRequestEncoder clearCacheEncoder = new ClearCounterCacheRequestEncoder();
    private final DeleteCounterCacheEncoder deleteCacheEncoder = new DeleteCounterCacheEncoder();
    private final RemoveCounterRequestEncoder removeCacheEntryEncoder = new RemoveCounterRequestEncoder();
    private final CancelCounterItemRemovalEncoder cancelItemRemovalEncoder = new CancelCounterItemRemovalEncoder();
    private final GetAllCounterCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCounterCacheEntriesEncoder();
    private final CounterCacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CounterCacheSubscriptionRequestEncoder();
    private final CounterCacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CounterCacheUnsubscribeRequestEncoder();
    private final IncrementCounterRequestEncoder incrementCounterRequestEncoder = new IncrementCounterRequestEncoder();
    private final DecrementCounterRequestEncoder decrementCounterRequestEncoder = new DecrementCounterRequestEncoder();
    private final SetCounterRequestEncoder setCounterRequestEncoder = new SetCounterRequestEncoder();
    private final GetCounterStatsEncoder getCacheStatsEncoder = new GetCounterStatsEncoder();

    @Override
    public int encodePatchValue(String requestId, String cacheId, String key, Long value, MutableDirectBuffer msgBuffer) {
        throw new UnsupportedOperationException("Patch value is not supported for counter caches");
    }

    @Override
    public int encodeIncrementCounterRequest(String requestId, String cacheId, String counterId, long amount, long ttl, MutableDirectBuffer msgBuffer) {
        incrementCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .amount(amount)
                .ttl(ttl)
                .cacheId(cacheId)
                .counterId(counterId)
                .requestId(requestId);
        return incrementCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDecrementCounterRequest(String requestId, String cacheId, String counterId, long amount, long ttl, MutableDirectBuffer msgBuffer) {
        decrementCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .amount(amount)
                .ttl(ttl)
                .cacheId(cacheId)
                .counterId(counterId)
                .requestId(requestId);
        return decrementCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeSetCounterRequest(String requestId, String cacheId, String counterId, long counterValue, long ttl, MutableDirectBuffer msgBuffer) {
        setCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .counterValue(counterValue)
                .ttl(ttl)
                .cacheId(cacheId)
                .counterId(counterId)
                .requestId(requestId);
        return setCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCreateCacheRequest(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return createCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntry(String requestId, String cacheId, String key, Long value, long ttl, MutableDirectBuffer msgBuffer) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .initialValue(value)
                .ttl(ttl)
                .cacheId(cacheId)
                .counterId(key)
                .requestId(requestId);
        return addCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntry(String requestId, String cacheId, String key, MutableDirectBuffer msgBuffer) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .key(key)
                .requestId(requestId);
        return getCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeClearCache(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return clearCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return deleteCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, String cacheId, String key, MutableDirectBuffer msgBuffer) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .counterId(key)
                .requestId(requestId);
        return removeCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCancelItemRemoval(String requestId, String cacheId, String key, MutableDirectBuffer msgBuffer) {
        cancelItemRemovalEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .counterId(key)
                .requestId(requestId);
        return cancelItemRemovalEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntries(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return getAllCacheEntriesEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<String> cacheId, List<String> keys, List<com.bhf.aeroncache.models.requests.SubscriptionMode> modes, boolean sendSnapshot, MutableDirectBuffer msgBuffer) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .sendSnapshot(sendSnapshot ? BooleanType.T : BooleanType.F);

        var itemsEncoder = cacheSubscriptionRequestEncoder.cacheIdsCount(cacheId.size());
        for (int i = 0; i < cacheId.size(); i++) {
            itemsEncoder.next();
            itemsEncoder.mode(modes.get(i) == com.bhf.aeroncache.models.requests.SubscriptionMode.PATCH ? SubscriptionMode.PATCH : SubscriptionMode.FULL);
            itemsEncoder.cacheId(cacheId.get(i));
            String key = keys.get(i);
            itemsEncoder.key(key == null ? "" : key);
        }

        cacheSubscriptionRequestEncoder.requestId(requestId);
        return cacheSubscriptionRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheUnsubscribe(String requestId, String cacheId, MutableDirectBuffer msgBuffer) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);
        return cacheUnsubscribeRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer) {
        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        return getCacheStatsEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer) {
        return 0;
    }
}
