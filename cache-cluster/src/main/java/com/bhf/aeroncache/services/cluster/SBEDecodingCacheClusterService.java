package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.services.cache.Cache;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.function.Supplier;

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
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final DeleteCacheDecoder deleteCacheDecoder = new DeleteCacheDecoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

    public SBEDecodingCacheClusterService() {
        super(SupplierUtils.longSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
    }

    /**
     * Decode the CreateCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CreateCacheRequestDetails flyweight.
     */
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

    /**
     * Decode the ClearCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The ClearCacheRequestDetails flyweight.
     */
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

    /**
     * Decode the RemoveCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The RemoveCacheEntryRequestDetails flyweight.
     */
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

    /**
     * Decode the AddCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The AddCacheEntryRequestDetails flyweight.
     */
    @Override
    protected AddCacheEntryRequestDetails<ReusableLong, ReusableString, ReusableString> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryRequestDetails.clear();
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.key();
        var value = addCacheEntryDecoder.entryValue();
        var requestID = addCacheEntryDecoder.requestId();
        addCacheEntryRequestDetails.getCacheId().copyFrom(cacheId);
        addCacheEntryRequestDetails.getKey().copyFrom(key);
        addCacheEntryRequestDetails.getValue().copyFrom(value);
        addCacheEntryRequestDetails.setRequestId(requestID);
        return addCacheEntryRequestDetails;

    }
    /**
     * Decode the GetCacheEntry message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The GetCacheEntryRequestDetails flyweight.
     */
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

    /**
     * Decode the DeleteCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The DeleteCacheRequestDetails flyweight.
     */
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

    /**
     * After the cache is created, send out a CacheCreated SBE message.
     *
     * @param cacheId             The ID of the cache created.
     * @param cacheCreationResult The result from the request to create the cache.
     * @param session             The client session.
     * @param buffer              The buffer from which the creation request was decoded.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostCreateCache(ReusableLong cacheId, CreateCacheResult<ReusableLong> cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheId(cacheId.getValue())
                .requestId(cacheCreationResult.getRequestId());
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * After an entry is added to a cache, send out a EntryCreated SBE message.
     *
     * @param cacheId             The ID of the cache in which the entry was created.
     * @param addCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param buffer              The buffer from which the entry creation request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostAddCacheEntry(ReusableLong cacheId, ReusableString key, ReusableString value, AddCacheEntryResult<ReusableLong, ReusableString> addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder.cacheId(cacheId.getValue())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * Get an entry from the cache, send out a CacheEntry SBE message.
     *
     * @param cacheId             The ID of the cache we need to get the entry from.
     * @param getCacheEntryResult The result from the request to add an entry.
     * @param session             The client session.
     * @param buffer              The buffer from which the entry creation request was created.
     * @param offset              The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostGetCacheEntry(ReusableLong cacheId, ReusableString key, GetCacheEntryResult<ReusableLong, ReusableString, ReusableString> getCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder.cacheId(cacheId.getValue())
                .key(getCacheEntryResult.getEntryKey().value())
                .requestId(getCacheEntryResult.getRequestId());
        
        if (getCacheEntryResult.getEntryValue() != null) {
            cacheEntryResultEncoder.value(getCacheEntryResult.getEntryValue().value());
        }

        sendMessage(session, egressBuffer, cacheEntryResultEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * After an entry is removed from the cache, send out a EntryRemoved SBE message.
     *
     * @param cacheId                The ID of the cache in which the entry was removed.
     * @param removeCacheEntryResult The result from the request to remove an entry.
     * @param session                The client session.
     * @param buffer                 The buffer from which the entry removal request was created.
     * @param offset                 The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostRemoveCacheEntry(ReusableLong cacheId, ReusableString key, RemoveCacheEntryResult<ReusableLong, ReusableString> removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder.cacheId(cacheId.getValue())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());
        sendMessage(session, egressBuffer, entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * After a cache is cleared, send out a CacheCleared SBE message.
     *
     * @param cacheId          The ID of the cache in which the entry was removed.
     * @param clearCacheResult The result from the request to clear a cache.
     * @param session          The client session.
     * @param buffer           The buffer from which the clear request was created.
     * @param offset           The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostClearCache(ReusableLong cacheId, ClearCacheResult<ReusableLong> clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder.cacheId(cacheId.getValue())
                .requestId(clearCacheResult.getRequestId());
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * After a cache is deleted, send out a CacheDeleted SBE message.
     *
     * @param cacheId           The ID of the cache which was deleted.
     * @param deleteCacheResult The deleted cache.
     * @param requestDetails    The original request to delete the cache.
     * @param session           The client session.
     * @param buffer            The buffer from which the delete request was created.
     * @param offset            The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostDeleteCache(ReusableLong cacheId, Cache<ReusableLong, ReusableString, ReusableString> deleteCacheResult, DeleteCacheRequestDetails<ReusableLong> requestDetails, ClientSession session, DirectBuffer buffer, int offset) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder.cacheId(cacheId.getValue())
                .requestId(requestDetails.getRequestId());
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
    }
}
