package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.codecs.CacheRequestDecoder;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.cluster.service.ClientSession;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Decode SBE messages representing cache actions. This level of
 * abstraction is an implementation which does have responsibility for
 * message decoding and encoding.
 */
@Log4j2
public class SBEDecodingCacheClusterService<I extends Reusable, K extends Reusable, V extends Reusable> extends AbstractCacheClusterService<I, K, V>{

    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();
    private final CacheRequestDecoder<I, K, V> decoder;
    private final com.bhf.aeroncache.codecs.CacheResponseEncoder<I, K, V> encoder;

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService, CacheManagerFactory<I, K, V> cacheManagerFactory) {
        super(nodeId, tracingService, cacheManagerFactory);
        this.decoder = cacheManagerFactory.getDecoder();
        this.encoder = cacheManagerFactory.getEncoder();
    }

    @Override
    protected CreateCacheRequestDetails<I> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCreateCacheRequestDetails(buffer, offset, createCacheRequestDetails);
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<I> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeClearCacheRequest(buffer, offset, clearCacheRequestDetails);
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<I, K> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeRemoveCacheEntryRequest(buffer, offset, removeCacheEntryRequestDetails);
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<I, K, V> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeAddCacheEntryRequest(buffer, offset, addCacheEntryRequestDetails);
        return addCacheEntryRequestDetails;
    }

    @Override
    protected GetCacheEntryRequestDetails<I, K> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheEntryRequest(buffer, offset, getCacheEntryRequestDetails);
        return getCacheEntryRequestDetails;
    }

    @Override
    protected GetAllCacheEntriesRequestDetails<I> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetAllCacheEntriesRequest(buffer, offset, getAllCacheEntriesRequestDetails);
        return getAllCacheEntriesRequestDetails;
    }

    @Override
    protected DeleteCacheRequestDetails<I> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetDeleteCacheRequest(buffer, offset, deleteCacheRequestDetails);
        return deleteCacheRequestDetails;
    }

    @Override
    protected GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheStatsRequest(buffer, offset, getCacheStatsRequestDetails);
        return getCacheStatsRequestDetails;
    }

    @Override
    protected CacheSubscriptionRequestDetails<I> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeCacheSubscriptionRequest(buffer, offset, cacheSubscribeRequestDetails);
        return cacheSubscribeRequestDetails;
    }

    @Override
    protected CacheUnsubscribeRequestDetails<I> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset) {
        decoder.decodeGetCacheUnsubscribeRequest(buffer, offset, cacheUnsubscribeRequestDetails);
        return cacheUnsubscribeRequestDetails;
    }

    @Override
    protected void handlePostCreateCache(I cacheId, CreateCacheResult<I> cacheCreationResult, ClientSession session) {
        var length = encoder.encodeCacheCreationResult(cacheId, cacheCreationResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostAddCacheEntry(I cacheId, K key, V value, AddCacheEntryResult<I, K> addCacheEntryResult, ClientSession session) {
        var addCacheEntryResultLength = encoder.encodeAddCacheEntryResult(cacheId, key, addCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, addCacheEntryResultLength);

        var entryUpdatedLength = encoder.encodeEntryUpdated(key, value, addCacheEntryResult, egressBuffer);
        subscriptionService.handleEntryAdded(addCacheEntryResult, egressBuffer, key, value, entryUpdatedLength);
    }

    @Override
    protected void handlePostGetCacheEntry(I cacheId, K key, GetCacheEntryResult<I, K, V> getCacheEntryResult, ClientSession session) {
        var length = encoder.encodeCacheEntryResult(cacheId, getCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostGetAllCacheEntries(I cacheId, GetAllCacheEntriesResult<I, K, V> getAllCacheEntriesResult, ClientSession session) {
        var length = encoder.encodeAllCacheEntriesResult(cacheId, getAllCacheEntriesResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult<I, K> removeCacheEntryResult, ClientSession session) {
        var length = encoder.encodeRemoveCacheEntryResult(cacheId, key, removeCacheEntryResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleEntryRemoved(removeCacheEntryResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostClearCache(I cacheId, ClearCacheResult<I> clearCacheResult, ClientSession session) {
        var length = encoder.encodeCacheCleared(cacheId, clearCacheResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleClearCache(clearCacheResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostDeleteCache(I cacheId, DeleteCacheResult<I> deleteCacheResult, DeleteCacheRequestDetails<I> requestDetails, ClientSession session) {
        var length = encoder.encodeDeleteCache(cacheId, deleteCacheResult, requestDetails, egressBuffer);
        sendMessage(session, egressBuffer, length);
        subscriptionService.handleDeleteCache(deleteCacheResult, egressBuffer, length, session.id());
    }

    @Override
    protected void handlePostGetCacheStats(CacheStatsResult<I> cacheStatsResult, ClientSession session) {
        var length = encoder.encodeCacheStatsResult(cacheStatsResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<I> subscriptionRequestResult, ClientSession session) {
        var length = encoder.encodeCacheSubscriptionResult(subscriptionRequestResult, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

    @Override
    protected void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<I> unsubscribeResponse, ClientSession session) {
        var length = encoder.encodeCacheUnsubscribeRequest(unsubscribeResponse, egressBuffer);
        sendMessage(session, egressBuffer, length);
    }

}
