package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;

import java.util.List;

public class ReusableStringCountersCacheRequestEncoder implements CountersCacheRequestEncoder<ReusableString, ReusableString, ReusableLong> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCounterCacheEncoder createCacheEncoder = new CreateCounterCacheEncoder();
    private final IncrementCounterRequestEncoder incrementCounterRequestEncoder = new IncrementCounterRequestEncoder();
    private final DecrementCounterRequestEncoder decrementCounterRequestEncoder = new DecrementCounterRequestEncoder();
    private final SetCounterRequestEncoder setCounterRequestEncoder = new SetCounterRequestEncoder();
    private final AddCounterRequestEncoder addCacheEntryEncoder = new AddCounterRequestEncoder();
    private final GetCounterCacheEntryEncoder getCacheEntryEncoder = new GetCounterCacheEntryEncoder();
    private final ClearCounterCacheRequestEncoder clearCacheEncoder = new ClearCounterCacheRequestEncoder();
    private final DeleteCounterCacheEncoder deleteCacheEncoder = new DeleteCounterCacheEncoder();
    private final RemoveCounterRequestEncoder removeCacheEntryEncoder = new RemoveCounterRequestEncoder();
    private final GetAllCounterCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCounterCacheEntriesEncoder();
    private final CounterCacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CounterCacheSubscriptionRequestEncoder();
    private final CounterCacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CounterCacheUnsubscribeRequestEncoder();


    @Override
    public int encodeIncrementCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long amount, long ttl, MutableDirectBuffer msgBuffer) {
        incrementCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .amount(amount)
                .ttl(ttl)
                .cacheId(cacheId.value())
                .counterId(counterId.value())
                .requestId(requestId);
        return incrementCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDecrementCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long amount, long ttl, MutableDirectBuffer msgBuffer) {
        decrementCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .amount(amount)
                .ttl(ttl)
                .cacheId(cacheId.value())
                .counterId(counterId.value())
                .requestId(requestId);
        return decrementCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeSetCounterRequest(String requestId, ReusableString cacheId, ReusableString counterId, long counterValue, long ttl, MutableDirectBuffer msgBuffer) {
        setCounterRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .counterValue(counterValue)
                .ttl(ttl);
        setCounterRequestEncoder.cacheId(cacheId.value());
        setCounterRequestEncoder.counterId(counterId.value());
        setCounterRequestEncoder.requestId(requestId);
        return setCounterRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCreateCacheRequest(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        createCacheEncoder.cacheId(cacheId.value());
        createCacheEncoder.requestId(requestId);
        return createCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableLong value, long ttl, MutableDirectBuffer msgBuffer) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .initialValue(value.value())
                .ttl(ttl);
        addCacheEntryEncoder.cacheId(cacheId.value());
        addCacheEntryEncoder.counterId(key.value());
        addCacheEntryEncoder.requestId(requestId);
        return addCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        getCacheEntryEncoder.cacheId(cacheId.value());
        getCacheEntryEncoder.key(key.value());
        getCacheEntryEncoder.requestId(requestId);
        return getCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeClearCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        clearCacheEncoder.cacheId(cacheId.value());
        clearCacheEncoder.requestId(requestId);
        return clearCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        deleteCacheEncoder.cacheId(cacheId.value());
        deleteCacheEncoder.requestId(requestId);
        return deleteCacheEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value())
                .counterId(key.value())
                .requestId(requestId);
        return removeCacheEntryEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntries(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        getAllCacheEntriesEncoder.cacheId(cacheId.value());
        getAllCacheEntriesEncoder.requestId(requestId);
        return getAllCacheEntriesEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<ReusableString> cacheId, boolean sendSnapshot, MutableDirectBuffer msgBuffer) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .sendSnapshot(sendSnapshot ? BooleanType.T : BooleanType.F);

        var itemsEncoder = cacheSubscriptionRequestEncoder.cacheIdsCount(cacheId.size());
        for (int i = 0; i < cacheId.size(); i++) {
            itemsEncoder.next();
            ReusableString cache = cacheId.get(i);
            itemsEncoder.cacheId(cache.value());
        }

        cacheSubscriptionRequestEncoder.requestId(requestId);
        return cacheSubscriptionRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheUnsubscribe(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        cacheUnsubscribeRequestEncoder.cacheId(cacheId.value());
        cacheUnsubscribeRequestEncoder.requestId(requestId);
        return cacheUnsubscribeRequestEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer) {
        return 0;
    }

    @Override
    public int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer) {
        return 0;
    }
}
