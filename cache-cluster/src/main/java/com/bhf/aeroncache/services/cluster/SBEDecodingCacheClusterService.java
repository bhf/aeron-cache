package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import com.bhf.aeroncache.types.ReusableLong;
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
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<ReusableLong, ReusableString, ReusableString> {

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
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

    public SBEDecodingCacheClusterService(String nodeId, CacheTracingService tracingService) {
        super(SupplierUtils.longSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, nodeId, tracingService);
    }

    @Override
    protected CreateCacheRequestDetails<ReusableLong> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = createCacheDecoder.cacheId();
        var requestId = createCacheDecoder.requestId();
        createCacheRequestDetails.getCacheId().copyFrom(cacheId);
        createCacheRequestDetails.setRequestId(requestId);
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<ReusableLong> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        clearCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = clearCacheDecoder.cacheId();
        var requestId = clearCacheDecoder.requestId();
        clearCacheRequestDetails.getCacheId().copyFrom(cacheId);
        clearCacheRequestDetails.setRequestId(requestId);
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<ReusableLong, ReusableString> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
        removeCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = removeCacheEntryDecoder.cacheId();
        var key = removeCacheEntryDecoder.key();
        var requestId = removeCacheEntryDecoder.requestId();
        removeCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        removeCacheEntryRequestDetails.getKey().copyFrom(key);
        removeCacheEntryRequestDetails.setRequestId(requestId);
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<ReusableLong, ReusableString, ReusableString> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryRequestDetails.clear();
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = addCacheEntryDecoder.cacheId();
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
    protected GetCacheEntryRequestDetails<ReusableLong, ReusableString> getCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
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
    protected GetAllCacheEntriesRequestDetails<ReusableLong> getAllCacheEntriesRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        getAllCacheEntriesRequestDetails.clear();
        getAllCacheEntriesDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = getAllCacheEntriesDecoder.cacheId();
        var requestId = getAllCacheEntriesDecoder.requestId();
        getAllCacheEntriesRequestDetails.getCacheId().copyFrom(cacheId);
        getAllCacheEntriesRequestDetails.setRequestId(requestId);
        return getAllCacheEntriesRequestDetails;
    }

    @Override
    protected DeleteCacheRequestDetails<ReusableLong> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        deleteCacheRequestDetails.clear();
        deleteCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = deleteCacheDecoder.cacheId();
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
    protected void handlePostCreateCache(ReusableLong cacheId, CreateCacheResult<ReusableLong> cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheId(cacheId.getValue())
                .status(cacheCreationResult.getStatus())
                .requestId(cacheCreationResult.getRequestId());
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostAddCacheEntry(ReusableLong cacheId, ReusableString key, ReusableString value, AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder.cacheId(cacheId.getValue())
                .status(addCacheEntryResult.getStatus())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostGetCacheEntry(ReusableLong cacheId, ReusableString key, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder.cacheId(cacheId.getValue())
                .status(getCacheEntryResult.getStatus())
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
    protected void handlePostGetAllCacheEntries(ReusableLong cacheId, GetAllCacheEntriesResult<ReusableLong, ReusableString, ReusableString> getAllCacheEntriesResult, ClientSession session, DirectBuffer buffer, int offset) {
        allCacheEntriesResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allCacheEntriesResultEncoder.cacheId(cacheId.getValue())
                .status(getAllCacheEntriesResult.getStatus())
                .endOfBatch(BooleanType.T);

        var values = getAllCacheEntriesResult.getValues();
        int size = values.size();
        var itemsEncoder = allCacheEntriesResultEncoder.itemsCount(size);

        values.forEach((key, value) -> {
            itemsEncoder.next();
            itemsEncoder.key(key.value()).value(value.value());
        });

        allCacheEntriesResultEncoder.requestId(getAllCacheEntriesResult.getRequestId());
        sendMessage(session, egressBuffer, allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostRemoveCacheEntry(ReusableLong cacheId, ReusableString key, RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder.cacheId(cacheId.getValue())
                .status(removeCacheEntryResult.getStatus())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostClearCache(ReusableLong cacheId, ClearCacheResult<ReusableLong> clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder.cacheId(cacheId.getValue())
                .status(clearCacheResult.getStatus())
                .requestId(clearCacheResult.getRequestId());
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostDeleteCache(ReusableLong cacheId, DeleteCacheResult<ReusableLong> deleteCacheResult, DeleteCacheRequestDetails<ReusableLong> requestDetails, ClientSession session, DirectBuffer buffer, int offset) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder.cacheId(cacheId.getValue())
                .status(deleteCacheResult.getStatus())
                .requestId(requestDetails.getRequestId());
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostGetCacheStats(CacheStatsResult<ReusableLong> cacheStatsResult, ClientSession session, DirectBuffer buffer, int offset) {
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
}
