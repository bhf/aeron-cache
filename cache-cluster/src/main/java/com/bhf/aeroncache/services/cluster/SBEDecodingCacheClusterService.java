package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
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
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<ReusableString, ReusableString, ReusableString> {

    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final ClearCacheDecoder clearCacheDecoder = new ClearCacheDecoder();
    private final RemoveCacheEntryDecoder removeCacheEntryDecoder = new RemoveCacheEntryDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final GetCacheEntryDecoder getCacheEntryDecoder = new GetCacheEntryDecoder();
    private final GetAllCacheEntriesDecoder getAllCacheEntriesDecoder = new GetAllCacheEntriesDecoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final AllCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCacheEntriesResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final DeleteCacheDecoder deleteCacheDecoder = new DeleteCacheDecoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final GetCacheStatsDecoder getCacheStatsDecoder = new GetCacheStatsDecoder();
    private final AllCacheStatsResultEncoder cacheStatsResultEncoder = new AllCacheStatsResultEncoder();
    private final CacheSubscriptionRequestDecoder cacheSubscriptionRequestDecoder = new CacheSubscriptionRequestDecoder();
    private final CacheUnsubscribeRequestDecoder cacheUnsubscribeRequestDecoder = new CacheUnsubscribeRequestDecoder();
    private final CacheSubscriptionResponseEncoder cacheSubscriptionResponseEncoder = new CacheSubscriptionResponseEncoder();
    private final CacheUnsubscribeResponseEncoder cacheUnsubscribeResponseEncoder = new CacheUnsubscribeResponseEncoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService) {
        super(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.hashmapSupplier, nodeId, tracingService);
    }

    @Override
    protected CreateCacheRequestDetails<ReusableString> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = createCacheDecoder.cacheId();
        var requestId = createCacheDecoder.requestId();
        createCacheRequestDetails.getCacheId().copyFrom(cacheId);
        createCacheRequestDetails.setRequestId(requestId);
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<ReusableString> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        clearCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = clearCacheDecoder.cacheId();
        var requestId = clearCacheDecoder.requestId();
        clearCacheRequestDetails.getCacheId().copyFrom(cacheId);
        clearCacheRequestDetails.setRequestId(requestId);
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<ReusableString, ReusableString> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
        removeCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = removeCacheEntryDecoder.cacheId();
        var key = removeCacheEntryDecoder.key();
        var requestId = removeCacheEntryDecoder.requestId();
        removeCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        removeCacheEntryRequestDetails.getKey().copyFrom(key);
        removeCacheEntryRequestDetails.setRequestId(requestId);
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<ReusableString, ReusableString, ReusableString> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
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
        return addCacheEntryRequestDetails;
    }

    @Override
    protected GetCacheEntryRequestDetails<ReusableString, ReusableString> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        getCacheEntryRequestDetails.clear();
        getCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getCacheEntryDecoder.cacheId();
        var key = getCacheEntryDecoder.key();
        var requestId = getCacheEntryDecoder.requestId();
        getCacheEntryRequestDetails.getKey().copyFrom(key);
        getCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        getCacheEntryRequestDetails.setRequestId(requestId);
        return getCacheEntryRequestDetails;
    }

    @Override
    protected GetAllCacheEntriesRequestDetails<ReusableString> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        getAllCacheEntriesRequestDetails.clear();
        getAllCacheEntriesDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getAllCacheEntriesDecoder.cacheId();
        var requestId = getAllCacheEntriesDecoder.requestId();
        getAllCacheEntriesRequestDetails.getCacheId().copyFrom(cacheId);
        getAllCacheEntriesRequestDetails.setRequestId(requestId);
        return getAllCacheEntriesRequestDetails;
    }

    @Override
    protected DeleteCacheRequestDetails<ReusableString> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        deleteCacheRequestDetails.clear();
        deleteCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = deleteCacheDecoder.cacheId();
        var requestId = deleteCacheDecoder.requestId();
        deleteCacheRequestDetails.getCacheId().copyFrom(cacheId);
        deleteCacheRequestDetails.setRequestId(requestId);
        return deleteCacheRequestDetails;
    }

    @Override
    protected GetCacheStatsRequestDetails getCacheStatsRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        getCacheStatsRequestDetails.clear();
        getCacheStatsDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var requestId = getCacheStatsDecoder.requestId();
        getCacheStatsRequestDetails.setRequestId(requestId);
        return getCacheStatsRequestDetails;
    }

    @Override
    protected CacheSubscriptionRequestDetails<ReusableString> getCacheSubscriptionRequest(ClientSession session, DirectBuffer buffer, int offset) {
        cacheSubscribeRequestDetails.clear();
        cacheSubscriptionRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheSubscriptionRequestDecoder.cacheId();
        var requestId = cacheSubscriptionRequestDecoder.requestId();
        cacheSubscribeRequestDetails.getCacheId().copyFrom(cacheId);
        cacheSubscribeRequestDetails.setRequestId(requestId);
        return cacheSubscribeRequestDetails;
    }

    @Override
    protected CacheUnsubscribeRequestDetails<ReusableString> getCacheUnsubscribeRequest(ClientSession session, DirectBuffer buffer, int offset) {
        cacheUnsubscribeRequestDetails.clear();
        cacheUnsubscribeRequestDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheUnsubscribeRequestDecoder.cacheId();
        var requestId = cacheUnsubscribeRequestDecoder.requestId();
        cacheUnsubscribeRequestDetails.getCacheId().copyFrom(cacheId);
        cacheUnsubscribeRequestDetails.setRequestId(requestId);
        return cacheUnsubscribeRequestDetails;
    }

    @Override
    protected void handlePostCreateCache(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder
                .status(cacheCreationResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(cacheCreationResult.getRequestId());
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostAddCacheEntry(ReusableString cacheId, ReusableString key, ReusableString value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder
                .status(addCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleEntryAdded(addCacheEntryResult, egressBuffer, key, value, entryCreatedEncoder, headerEncoder);
    }

    @Override
    protected void handlePostGetCacheEntry(ReusableString cacheId, ReusableString key, GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
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
    protected void handlePostGetAllCacheEntries(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllCacheEntriesResult, ClientSession session, DirectBuffer buffer, int offset) {
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
    protected void handlePostRemoveCacheEntry(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder
                .status(removeCacheEntryResult.getStatus())
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleEntryRemoved(removeCacheEntryResult, egressBuffer, entryRemovedEncoder, headerEncoder, session.id());
    }

    @Override
    protected void handlePostClearCache(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder
                .status(clearCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(clearCacheResult.getRequestId());
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleClearCache(clearCacheResult, egressBuffer, cacheClearedEncoder, headerEncoder, session.id());
    }

    @Override
    protected void handlePostDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, DeleteCacheRequestDetails<ReusableString> requestDetails, ClientSession session, DirectBuffer buffer, int offset) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder
                .status(deleteCacheResult.getStatus())
                .cacheId(cacheId.value())
                .requestId(requestDetails.getRequestId());
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
        subscriptionService.handleDeleteCache(deleteCacheResult, egressBuffer, cacheDeletedEncoder, headerEncoder, session.id());
    }

    @Override
    protected void handlePostGetCacheStats(CacheStatsResult<ReusableString> cacheStatsResult, ClientSession session, DirectBuffer buffer, int offset) {
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
    protected void handlePostCacheSubscriptionRequest(CacheSubscriptionResult<ReusableString> subscriptionRequestResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheSubscriptionResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheSubscriptionResponseEncoder
                .status(subscriptionRequestResult.getStatus())
                .cacheId(subscriptionRequestResult.getCacheId().value())
                .requestId(subscriptionRequestResult.getRequestId());
        sendMessage(session, egressBuffer, cacheSubscriptionResponseEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostCacheUnsubscribeRequest(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, ClientSession session, DirectBuffer buffer, int offset) {
        cacheUnsubscribeResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheUnsubscribeResponseEncoder
                .status(unsubscribeResponse.getStatus())
                .cacheId(unsubscribeResponse.getCacheId().value())
                .requestId(unsubscribeResponse.getRequestId());
        sendMessage(session, egressBuffer, cacheUnsubscribeResponseEncoder.encodedLength() + headerEncoder.encodedLength());
    }
}
