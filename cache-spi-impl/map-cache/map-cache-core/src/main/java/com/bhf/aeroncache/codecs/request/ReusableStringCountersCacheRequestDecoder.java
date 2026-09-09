package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

public class ReusableStringCountersCacheRequestDecoder implements CountersCacheRequestDecoder<ReusableString, ReusableString, ReusableLong> {

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CreateCounterCacheDecoder createCacheDecoder = new CreateCounterCacheDecoder();
    private final CounterCacheSubscriptionRequestDecoder cacheSubscriptionRequestDecoder = new CounterCacheSubscriptionRequestDecoder();
    private final ClearCounterCacheRequestDecoder clearCacheDecoder = new ClearCounterCacheRequestDecoder();
    private final RemoveCounterRequestDecoder removeCacheEntryDecoder = new RemoveCounterRequestDecoder();
    private final CancelCounterItemRemovalDecoder cancelItemRemovalDecoder = new CancelCounterItemRemovalDecoder();
    private final AddCounterRequestDecoder addCacheEntryDecoder = new AddCounterRequestDecoder();
    private final GetCounterCacheEntryDecoder getCacheEntryDecoder = new GetCounterCacheEntryDecoder();
    private final GetAllCounterCacheEntriesDecoder getAllCacheEntriesDecoder = new GetAllCounterCacheEntriesDecoder();
    private final DeleteCounterCacheDecoder deleteCacheDecoder = new DeleteCounterCacheDecoder();
    private final GetCounterStatsDecoder getCacheStatsDecoder = new GetCounterStatsDecoder();
    private final CounterCacheUnsubscribeRequestDecoder cacheUnsubscribeRequestDecoder = new CounterCacheUnsubscribeRequestDecoder();
    private final BulkOperationRequestDecoder bulkOperationRequestDecoder = new BulkOperationRequestDecoder();
    private final IncrementCounterRequestDecoder incrementCounterRequestDecoder = new IncrementCounterRequestDecoder();
    private final DecrementCounterRequestDecoder decrementCounterRequestDecoder = new DecrementCounterRequestDecoder();
    private final SetCounterRequestDecoder setCounterRequestDecoder = new SetCounterRequestDecoder();

    @Override
    public <VT extends Reusable> void decodePatchValueRequest(DirectBuffer buffer, int offset, PatchValueRequestDetails<ReusableString, ReusableString, VT> patchValueRequestDetails) {
        throw new UnsupportedOperationException("Patch value is not supported for counter caches");
    }

    @Override
    public void decodeGetCreateCacheRequestDetails(DirectBuffer buffer, int offset, CreateCacheRequestDetails<ReusableString> createCacheRequestDetails) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = createCacheDecoder.cacheId();
        createCacheRequestDetails.getCacheId().copyFrom(cacheId);

        var requestId = createCacheDecoder.requestId();
        createCacheRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeCacheSubscriptionRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString, ReusableString> cacheSubscribeRequestDetails) {
        cacheSubscribeRequestDetails.clear();
        cacheSubscriptionRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var sendSnapshot = cacheSubscriptionRequestDecoder.sendSnapshot();
        boolean shouldSendInitialState = sendSnapshot==BooleanType.T;

        cacheSubscribeRequestDetails.getCacheId().clear();
        var cacheIds = cacheSubscribeRequestDetails.getCacheId();
        var keys = cacheSubscribeRequestDetails.getSubscriptionKey();
        var modes = cacheSubscribeRequestDetails.getSubscriptionMode();

        var itemsDecoder = cacheSubscriptionRequestDecoder.cacheIds();

        for(var op: itemsDecoder) {
            var mode = op.mode();
            ReusableString reusableCacheId = new ReusableString();
            var cacheId = op.cacheId();
            reusableCacheId.copyFrom(cacheId);
            cacheIds.add(reusableCacheId);

            var key = op.key();
            if (key == null || key.isEmpty()) {
                keys.add(null);
            } else {
                ReusableString reusableKey = new ReusableString();
                reusableKey.copyFrom(key);
                keys.add(reusableKey);
            }

            modes.add(mode == com.bhf.aeroncache.messages.SubscriptionMode.PATCH
                    ? com.bhf.aeroncache.models.requests.SubscriptionMode.PATCH
                    : com.bhf.aeroncache.models.requests.SubscriptionMode.FULL);
        }

        var requestId = cacheSubscriptionRequestDecoder.requestId();
        cacheSubscribeRequestDetails.setSendSnapshot(shouldSendInitialState);
        cacheSubscribeRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeClearCacheRequest(DirectBuffer buffer, int offset, ClearCacheRequestDetails<ReusableString> clearCacheRequestDetails) {
        clearCacheRequestDetails.clear();
        clearCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = clearCacheDecoder.cacheId();
        var requestId = clearCacheDecoder.requestId();
        clearCacheRequestDetails.getCacheId().copyFrom(cacheId);
        clearCacheRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeRemoveCacheEntryRequest(DirectBuffer buffer, int offset, RemoveCacheEntryRequestDetails<ReusableString, ReusableString> removeCacheEntryRequestDetails) {
        removeCacheEntryRequestDetails.clear();
        removeCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = removeCacheEntryDecoder.cacheId();
        var key = removeCacheEntryDecoder.counterId();
        var requestId = removeCacheEntryDecoder.requestId();
        removeCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        removeCacheEntryRequestDetails.getKey().copyFrom(key);
        removeCacheEntryRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeCancelItemRemovalRequest(DirectBuffer buffer, int offset, CancelItemRemovalRequestDetails<ReusableString, ReusableString> cancelItemRemovalRequestDetails) {
        cancelItemRemovalRequestDetails.clear();
        cancelItemRemovalDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cancelItemRemovalDecoder.cacheId();
        var key = cancelItemRemovalDecoder.counterId();
        var requestId = cancelItemRemovalDecoder.requestId();
        cancelItemRemovalRequestDetails.getCacheId().copyFrom(cacheId);
        cancelItemRemovalRequestDetails.getKey().copyFrom(key);
        cancelItemRemovalRequestDetails.setRequestId(requestId);
    }

    @Override
    public <VT extends Reusable> void decodeAddCacheEntryRequest(DirectBuffer buffer, int offset, AddCacheEntryRequestDetails<ReusableString, ReusableString, VT> addCacheEntryRequestDetails) {
        addCacheEntryRequestDetails.clear();
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var counterValue = addCacheEntryDecoder.initialValue();
        var ttl = addCacheEntryDecoder.ttl();
        var cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.counterId();
        var requestId = addCacheEntryDecoder.requestId();

        addCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        addCacheEntryRequestDetails.getKey().copyFrom(key);
        addCacheEntryRequestDetails.getValue().copyFrom(counterValue);
        addCacheEntryRequestDetails.setTtl(ttl);
        addCacheEntryRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeGetCacheEntryRequest(DirectBuffer buffer, int offset, GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails) {
        getCacheEntryRequestDetails.clear();
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getCacheEntryDecoder.cacheId();
        var key = getCacheEntryDecoder.key();
        var requestId = getCacheEntryDecoder.requestId();
        getCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        getCacheEntryRequestDetails.getKey().copyFrom(key);
        getCacheEntryRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeGetAllCacheEntriesRequest(DirectBuffer buffer, int offset, GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails) {
        getAllCacheEntriesRequestDetails.clear();
        getAllCacheEntriesDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getAllCacheEntriesDecoder.cacheId();
        var requestId = getAllCacheEntriesDecoder.requestId();
        getAllCacheEntriesRequestDetails.getCacheId().copyFrom(cacheId);
        getAllCacheEntriesRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeGetDeleteCacheRequest(DirectBuffer buffer, int offset, DeleteCacheRequestDetails<ReusableString> deleteCacheRequestDetails) {
        deleteCacheRequestDetails.clear();
        deleteCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = deleteCacheDecoder.cacheId();
        var requestId = deleteCacheDecoder.requestId();
        deleteCacheRequestDetails.getCacheId().copyFrom(cacheId);
        deleteCacheRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeGetCacheStatsRequest(DirectBuffer buffer, int offset, GetCacheStatsRequestDetails getCacheStatsRequestDetails) {
        getCacheStatsRequestDetails.clear();
        getCacheStatsDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var requestId = getCacheStatsDecoder.requestId();
        getCacheStatsRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeGetCacheUnsubscribeRequest(DirectBuffer buffer, int offset, CacheUnsubscribeRequestDetails<ReusableString> cacheUnsubscribeRequestDetails) {
        cacheUnsubscribeRequestDetails.clear();
        cacheUnsubscribeRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheUnsubscribeRequestDecoder.cacheId();
        var requestId = cacheUnsubscribeRequestDecoder.requestId();
        cacheUnsubscribeRequestDetails.getCacheId().clear();
        cacheUnsubscribeRequestDetails.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeBulkCacheOperationsRequest(DirectBuffer buffer, int offset, BulkCacheOpsRequestDetails<ReusableString, ReusableString, ReusableLong> bulkCacheOpsRequestDetails) {

    }

    @Override
    public void decodeIncrementCounterRequest(DirectBuffer buffer, int offset, IncrementCounterRequestDetails<ReusableString, ReusableString> incrementCounterRequest) {
        incrementCounterRequest.clear();
        incrementCounterRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var amount = incrementCounterRequestDecoder.amount();
        var ttl = incrementCounterRequestDecoder.ttl();
        var cacheId = incrementCounterRequestDecoder.cacheId();
        var counterId = incrementCounterRequestDecoder.counterId();
        var requestId = incrementCounterRequestDecoder.requestId();
        incrementCounterRequest.setAmount(amount);
        incrementCounterRequest.getCacheId().copyFrom(cacheId);
        incrementCounterRequest.getCounterId().copyFrom(counterId);
        incrementCounterRequest.setTtl(ttl);
        incrementCounterRequest.setRequestId(requestId);
    }

    @Override
    public void decodeDecrementCounterRequest(DirectBuffer buffer, int offset, DecrementCounterRequestDetails<ReusableString, ReusableString> decrementCounterRequest) {
        decrementCounterRequest.clear();
        decrementCounterRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var amount = decrementCounterRequestDecoder.amount();
        var ttl = decrementCounterRequestDecoder.ttl();
        var cacheId = decrementCounterRequestDecoder.cacheId();
        var counterId = decrementCounterRequestDecoder.counterId();
        var requestId = decrementCounterRequestDecoder.requestId();
        decrementCounterRequest.setAmount(amount);
        decrementCounterRequest.getCacheId().copyFrom(cacheId);
        decrementCounterRequest.getCounterId().copyFrom(counterId);
        decrementCounterRequest.setTtl(ttl);
        decrementCounterRequest.setRequestId(requestId);
    }

    @Override
    public void decodeSetCounterRequest(DirectBuffer buffer, int offset, SetCounterRequestDetails<ReusableString, ReusableString> setCounterRequest) {
        setCounterRequest.clear();
        setCounterRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var counterValue = setCounterRequestDecoder.counterValue();
        var ttl = setCounterRequestDecoder.ttl();
        var cacheId = setCounterRequestDecoder.cacheId();
        var counterId = setCounterRequestDecoder.counterId();
        var requestId = setCounterRequestDecoder.requestId();
        setCounterRequest.setCounterValue(counterValue);
        setCounterRequest.getCacheId().copyFrom(cacheId);
        setCounterRequest.getCounterId().copyFrom(counterId);
        setCounterRequest.setTtl(ttl);
        setCounterRequest.setRequestId(requestId);
    }
}
