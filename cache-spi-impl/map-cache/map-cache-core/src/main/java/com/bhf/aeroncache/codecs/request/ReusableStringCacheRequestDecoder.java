package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.codecs.AppendableFlyweight;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.DirectBuffer;

public class ReusableStringCacheRequestDecoder implements CacheRequestDecoder<ReusableString, ReusableString, ReusableString> {

    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final CacheSubscriptionRequestDecoder cacheSubscriptionRequestDecoder = new CacheSubscriptionRequestDecoder();
    private final ClearCacheDecoder clearCacheDecoder = new ClearCacheDecoder();
    private final RemoveCacheEntryDecoder removeCacheEntryDecoder = new RemoveCacheEntryDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final GetCacheEntryDecoder getCacheEntryDecoder = new GetCacheEntryDecoder();
    private final GetAllCacheEntriesDecoder getAllCacheEntriesDecoder = new GetAllCacheEntriesDecoder();
    private final DeleteCacheDecoder deleteCacheDecoder = new DeleteCacheDecoder();
    private final GetCacheStatsDecoder getCacheStatsDecoder = new GetCacheStatsDecoder();
    private final CacheUnsubscribeRequestDecoder cacheUnsubscribeRequestDecoder = new CacheUnsubscribeRequestDecoder();
    private final BulkOperationRequestDecoder bulkOperationRequestDecoder = new BulkOperationRequestDecoder();

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
    public void decodeCacheSubscriptionRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {
        cacheSubscribeRequestDetails.clear();
        cacheSubscriptionRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var sendSnapshot = cacheSubscriptionRequestDecoder.sendSnapshot();
        boolean shouldSendInitialState = sendSnapshot==BooleanType.T;

        cacheSubscribeRequestDetails.getCacheId().clear();
        var cacheIds = cacheSubscribeRequestDetails.getCacheId();

        var itemsDecoder = cacheSubscriptionRequestDecoder.cacheIds();

        for(var op: itemsDecoder) {
            ReusableString reusableCacheId = new ReusableString();
            var cacheId = op.cacheId();
            reusableCacheId.copyFrom(cacheId);
            cacheIds.add(reusableCacheId);
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
        var key = removeCacheEntryDecoder.key();
        var requestId = removeCacheEntryDecoder.requestId();
        removeCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        removeCacheEntryRequestDetails.getKey().copyFrom(key);
        removeCacheEntryRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeAddCacheEntryRequest(DirectBuffer buffer, int offset, AddCacheEntryRequestDetails<ReusableString, ReusableString, ReusableString> addCacheEntryRequestDetails) {
            addCacheEntryRequestDetails.clear();
            addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
            var ttl = addCacheEntryDecoder.ttl();
            var cacheId = addCacheEntryDecoder.cacheId();
            var requestID = addCacheEntryDecoder.requestId();
            var key = addCacheEntryDecoder.key();
            var value = addCacheEntryDecoder.entryValue();
            addCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
            addCacheEntryRequestDetails.getKey().copyFrom(key);
            addCacheEntryRequestDetails.getValue().copyFrom(value);
            addCacheEntryRequestDetails.setRequestId(requestID);
            addCacheEntryRequestDetails.setTtl(ttl);
    }

    @Override
    public void decodeGetCacheEntryRequest(DirectBuffer buffer, int offset, GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails) {
        getCacheEntryRequestDetails.clear();
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getCacheEntryDecoder.cacheId();
        var key = getCacheEntryDecoder.key();
        var requestId = getCacheEntryDecoder.requestId();
        getCacheEntryRequestDetails.getKey().copyFrom(key);
        getCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
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
    public void decodeBulkCacheOperationsRequest(DirectBuffer buffer, int offset, BulkCacheOpsRequestDetails<ReusableString, ReusableString, ReusableString> bulkCacheOpsRequestDetails) {
        bulkCacheOpsRequestDetails.clear();
        bulkOperationRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var itemsDecoder = bulkOperationRequestDecoder.items();

        for(var op: itemsDecoder) {
            var opType = op.operationType();
            var ttl = op.ttl();
            var requestId = op.requestId();
            var cacheId = op.cacheId();
            var key = op.key();
            var value = op.value();
            ReusableString reusableCacheId = new ReusableString();
            ReusableString reusableKey = new ReusableString();
            ReusableString reusableValue = new ReusableString();
            reusableCacheId.copyFrom(cacheId);
            reusableKey.copyFrom(key);
            reusableValue.copyFrom(value);

            bulkCacheOpsRequestDetails.addOperation(
                    com.bhf.aeroncache.models.bulk.requests.BulkOperationType.valueOf(opType.toString()),
                    ttl, requestId, reusableCacheId, reusableKey, reusableValue);
        }

        var requestId = bulkOperationRequestDecoder.requestId();
        bulkCacheOpsRequestDetails.setRequestId(requestId);
    }

}
