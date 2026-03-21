package com.bhf.aeroncache.codecs;

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
    private final AppendableFlyweight appendable = new AppendableFlyweight();
    private boolean useAppendable = false;

    @Override
    public void decodeGetCreateCacheRequestDetails(DirectBuffer buffer, int offset, CreateCacheRequestDetails<ReusableString> createCacheRequestDetails) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        appendable.setReusable(createCacheRequestDetails.getCacheId());
        createCacheDecoder.cacheId(appendable);
        //var cacheId = createCacheDecoder.cacheId();
        var requestId = createCacheDecoder.requestId();
        createCacheRequestDetails.setRequestId(requestId);
    }

    @Override
    public void decodeCacheSubscriptionRequest(DirectBuffer buffer, int offset, CacheSubscriptionRequestDetails<ReusableString> cacheSubscribeRequestDetails) {
        cacheSubscribeRequestDetails.clear();
        cacheSubscriptionRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheSubscriptionRequestDecoder.cacheId();
        var requestId = cacheSubscriptionRequestDecoder.requestId();
        cacheSubscribeRequestDetails.getCacheId().clear();
        cacheSubscribeRequestDetails.getCacheId().copyFrom(cacheId);
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

        if(useAppendable) {
            addCacheEntryRequestDetails.clear();
            addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
            appendable.setReusable(addCacheEntryRequestDetails.getCacheId());
            addCacheEntryDecoder.cacheId(appendable);
            var requestID = addCacheEntryDecoder.requestId();
            addCacheEntryRequestDetails.setRequestId(requestID);
            appendable.setReusable(addCacheEntryRequestDetails.getKey());
            addCacheEntryDecoder.key(appendable);
            appendable.setReusable(addCacheEntryRequestDetails.getValue());
            addCacheEntryDecoder.entryValue(appendable);
        }
        else{
            addCacheEntryRequestDetails.clear();
            addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
            var cacheId = addCacheEntryDecoder.cacheId();
            var requestID = addCacheEntryDecoder.requestId();
            var key = addCacheEntryDecoder.key();
            var value = addCacheEntryDecoder.entryValue();
            addCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
            addCacheEntryRequestDetails.getKey().copyFrom(key);
            addCacheEntryRequestDetails.getValue().copyFrom(value);
            addCacheEntryRequestDetails.setRequestId(requestID);
        }
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

}
