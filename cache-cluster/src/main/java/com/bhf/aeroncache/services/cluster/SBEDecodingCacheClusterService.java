package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Decode SBE messages representing cache actions. This implementation
 * indexes the cache via it's {@link java.lang.Long} identity, with
 * keys and values being represented by String objects. This level of
 * abstraction is an implementation which does have responsibility for
 * message decoding.
 */
@Log4j2
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<ReusableString, ReusableString, ReusableString>{

    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();
    private final CacheRequestDecoder<ReusableString,ReusableString,ReusableString> decoder = new ReusableStringCacheRequestDecoder();
    private final CacheResponseEncoder<ReusableString, ReusableString, ReusableString> encoder = new ReusableStringCacheResponseEncoder();

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory) {
        super(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier,
                nodeId, tracingService, cacheManagerFactory);
    }

    @Override
    protected CreateCacheRequestDetails<ReusableString> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCreateCacheRequestDetails(buffer, offset, createCacheRequestDetails);
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<ReusableString> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeClearCacheRequest(buffer, offset, clearCacheRequestDetails);
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<ReusableString, ReusableString> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeRemoveCacheEntryRequest(buffer, offset, removeCacheEntryRequestDetails);
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<ReusableString, ReusableString, ReusableString> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeAddCacheEntryRequest(buffer, offset, addCacheEntryRequestDetails);
        return addCacheEntryRequestDetails;
    }

    @Override
    protected GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheEntryRequest(buffer, offset, getCacheEntryRequestDetails);
        return getCacheEntryRequestDetails;
    }

    @Override
    protected GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetAllCacheEntriesRequest(buffer, offset, getAllCacheEntriesRequestDetails);
        return getAllCacheEntriesRequestDetails;
    }

    @Override
    protected DeleteCacheRequestDetails<ReusableString> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetDeleteCacheRequest(buffer, offset, deleteCacheRequestDetails);
        return deleteCacheRequestDetails;
    }

    @Override
    protected GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheStatsRequest(buffer, offset, getCacheStatsRequestDetails);
        return getCacheStatsRequestDetails;
    }

    @Override
    protected CacheSubscriptionRequestDetails<ReusableString> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeCacheSubscriptionRequest(buffer, offset, cacheSubscribeRequestDetails);
        return cacheSubscribeRequestDetails;
    }

    @Override
    protected CacheUnsubscribeRequestDetails<ReusableString> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheUnsubscribeRequest(buffer, offset, cacheUnsubscribeRequestDetails);
        return cacheUnsubscribeRequestDetails;
    }

    @Override
    protected void handlePostCreateCache(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, ClientSession session) {
        var length = encoder.encodeCacheCreationResult(cacheId, cacheCreationResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostAddCacheEntry(ReusableString cacheId, ReusableString key, ReusableString value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, ClientSession session) {
        var addCacheEntryResultLength = encoder.encodeAddCacheEntryResult(cacheId, key, addCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, addCacheEntryResultLength);

        var entryUpdatedLength = encoder.encodeEntryUpdated(key, value, addCacheEntryResult, egressBuffer);
        subscriptionService.handleEntryAdded(addCacheEntryResult, egressBuffer, key, value, entryUpdatedLength);
    }

    @Override
    protected void handlePostGetCacheEntry(ReusableString cacheId, ReusableString key, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, ClientSession session) {
        var length = encoder.encodeCacheEntryResult(cacheId, getCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostGetAllCacheEntries(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllCacheEntriesResult, ClientSession session) {
        var length = encoder.encodeAllCacheEntriesResult(cacheId, getAllCacheEntriesResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostRemoveCacheEntry(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, ClientSession session) {
        var length = encoder.encodeRemoveCacheEntryResult(cacheId, key, removeCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleEntryRemoved(removeCacheEntryResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostClearCache(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, ClientSession session) {
        var length = encoder.encodeCacheCleared(cacheId, clearCacheResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleClearCache(clearCacheResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, DeleteCacheRequestDetails<ReusableString> requestDetails, ClientSession session) {
        var length = encoder.encodeDeleteCache(cacheId, deleteCacheResult, requestDetails, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleDeleteCache(deleteCacheResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostGetCacheStats(CacheStatsResult<ReusableString> cacheStatsResult, ClientSession session) {
        var length = encoder.encodeCacheStatsResult(cacheStatsResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<ReusableString> subscriptionRequestResult, ClientSession session) {
        var length = encoder.encodeCacheSubscriptionResult(subscriptionRequestResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, ClientSession session) {
        var length = encoder.encodeCacheUnsubscribeRequest(unsubscribeResponse, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

}
