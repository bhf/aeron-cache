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
import org.apache.logging.log4j.util.Strings;

/**
 * Decode SBE messages representing cache actions. This implementation
 * indexes the cache via it's {@link java.lang.Long} identity, with
 * keys and values being represented by String objects. This level of
 * abstraction is an implementation which does have responsibility for
 * message decoding.
 */
@Log4j2
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<ReusableString, ReusableString, ReusableString>{

    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final AllCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCacheEntriesResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final AllCacheStatsResultEncoder cacheStatsResultEncoder = new AllCacheStatsResultEncoder();
    private final CacheSubscriptionResponseEncoder cacheSubscriptionResponseEncoder = new CacheSubscriptionResponseEncoder();
    private final CacheUnsubscribeResponseEncoder cacheUnsubscribeResponseEncoder = new CacheUnsubscribeResponseEncoder();
    private final CacheEntryUpdateEncoder entryUpdateEncoder = new CacheEntryUpdateEncoder();

    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

    private CacheRequestDecoder<ReusableString,ReusableString,ReusableString> decoder;

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
        var length = encodeCacheCreationResult(cacheId, cacheCreationResult);
        sendMessage(session, egressBuffer, length);
    }

    private int encodeCacheCreationResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder
                .status(cacheCreationResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(cacheCreationResult.getRequestId());
        return cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    protected void handlePostAddCacheEntry(ReusableString cacheId, ReusableString key, ReusableString value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, ClientSession session) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder
                .status(addCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());

        entryUpdateEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryUpdateEncoder.cacheId((String) addCacheEntryResult.getCacheId().value())
                .key(key.value())
                .value(value.value())
                .requestId(addCacheEntryResult.getRequestId());

        subscriptionService.handleEntryAdded(addCacheEntryResult, egressBuffer, key, value, entryUpdateEncoder.encodedLength()+headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostGetCacheEntry(ReusableString cacheId, ReusableString key, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, ClientSession session) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder
                .status(getCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(getCacheEntryResult.getEntryKey().value());

        if (getCacheEntryResult.getEntryValue() != null) {
            cacheEntryResultEncoder.value(getCacheEntryResult.getEntryValue().value());
        } else {
            cacheEntryResultEncoder.value(Strings.EMPTY);
        }

        cacheEntryResultEncoder.requestId(getCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, cacheEntryResultEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostGetAllCacheEntries(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllCacheEntriesResult, ClientSession session) {
        allCacheEntriesResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allCacheEntriesResultEncoder
                .status(getAllCacheEntriesResult.getStatus())
                .endOfBatch(BooleanType.T);

        var values = getAllCacheEntriesResult.getValues();
        int size = values.size();
        var itemsEncoder = allCacheEntriesResultEncoder.itemsCount(size);

        values.forEach((key, value) -> {
            itemsEncoder.next();
            itemsEncoder.key(key.value()).value(value.value());
        });

        allCacheEntriesResultEncoder
                .requestId(getAllCacheEntriesResult.getRequestId())
                .cacheId(cacheId.value());
        sendMessage(session, egressBuffer, allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostRemoveCacheEntry(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, ClientSession session) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder
                .status(removeCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleEntryRemoved(removeCacheEntryResult, egressBuffer, entryRemovedEncoder.encodedLength()+headerEncoder.encodedLength(), session.id());
    }

    @Override
    protected void handlePostClearCache(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, ClientSession session) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder
                .status(clearCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(clearCacheResult.getRequestId());
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleClearCache(clearCacheResult, egressBuffer, cacheClearedEncoder.encodedLength()+headerEncoder.encodedLength(), session.id());
    }

    @Override
    protected void handlePostDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, DeleteCacheRequestDetails<ReusableString> requestDetails, ClientSession session) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder
                .status(deleteCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(requestDetails.getRequestId());
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleDeleteCache(deleteCacheResult, egressBuffer, cacheDeletedEncoder.encodedLength()+headerEncoder.encodedLength(), session.id());
    }

    @Override
    protected void handlePostGetCacheStats(CacheStatsResult<ReusableString> cacheStatsResult, ClientSession session) {
        cacheStatsResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheStatsResultEncoder.status(OperationStatus.SUCCESS);

        var values = cacheStatsResult.getStats();
        int size = values.size();
        var itemsEncoder = cacheStatsResultEncoder.statsCount(size);
        values.forEach(v -> {
            itemsEncoder.next();
            itemsEncoder.added(v.addedCount);
            itemsEncoder.removed(v.removedCount);
            itemsEncoder.cleared(v.clearedCount);
            itemsEncoder.size(v.size);
            itemsEncoder.cacheId(v.getCacheId().value());
        });

        cacheStatsResultEncoder.requestId(cacheStatsResult.getRequestId());
        sendMessage(session, egressBuffer, cacheStatsResultEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<ReusableString> subscriptionRequestResult, ClientSession session) {
        cacheSubscriptionResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheSubscriptionResponseEncoder
                .status(subscriptionRequestResult.getStatus())
                .cacheId(subscriptionRequestResult.getCacheId().value())
                .requestId(subscriptionRequestResult.getRequestId());
        sendMessage(session, egressBuffer, cacheSubscriptionResponseEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, ClientSession session) {
        cacheUnsubscribeResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheUnsubscribeResponseEncoder
                .status(unsubscribeResponse.getStatus())
                .cacheId(unsubscribeResponse.getCacheId().value())
                .requestId(unsubscribeResponse.getRequestId());
        sendMessage(session, egressBuffer, cacheUnsubscribeResponseEncoder.encodedLength() + headerEncoder.encodedLength());
    }
}
