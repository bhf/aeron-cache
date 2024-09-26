package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.Cache;
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
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<Long, String, String> {

    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final ClearCacheDecoder clearCacheDecoder = new ClearCacheDecoder();
    private final RemoveCacheEntryDecoder removeCacheEntryDecoder = new RemoveCacheEntryDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final DeleteCacheDecoder deleteCacheDecoder = new DeleteCacheDecoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();


    /**
     * Decode the CreateCache message into a request details flyweight.
     *
     * @param session The client session.
     * @param buffer  The buffer to decode from.
     * @param offset  The offset from within the buffer to decode from.
     * @return The CreateCacheRequestDetails flyweight.
     */
    @Override
    protected CreateCacheRequestDetails<Long> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = createCacheDecoder.cacheId();
        createCacheRequestDetails.setCacheId(cacheId);
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
    protected ClearCacheRequestDetails<Long> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        clearCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = clearCacheDecoder.cacheId();
        clearCacheRequestDetails.setCacheId(cacheId);
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
    protected RemoveCacheEntryRequestDetails<Long, String> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
        removeCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = removeCacheEntryDecoder.cacheId();
        var key = removeCacheEntryDecoder.key();
        removeCacheEntryRequestDetails.setCacheId(cacheId);
        removeCacheEntryRequestDetails.setKey(key);
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
    protected AddCacheEntryRequestDetails<Long, String, String> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryRequestDetails.clear();
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = addCacheEntryDecoder.cacheId();
        var key = addCacheEntryDecoder.key();
        var value = addCacheEntryDecoder.entryValue();
        addCacheEntryRequestDetails.setCacheId(cacheId);
        addCacheEntryRequestDetails.setKey(key);
        addCacheEntryRequestDetails.setValue(value);
        return addCacheEntryRequestDetails;
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
    protected DeleteCacheRequestDetails<Long> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        deleteCacheRequestDetails.clear();
        deleteCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = deleteCacheDecoder.cacheId();
        deleteCacheRequestDetails.setCacheId(cacheId);
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
    protected void handlePostCreateCache(Long cacheId, CreateCacheResult<Long> cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheId(cacheId);
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
    protected void handlePostAddCacheEntry(Long cacheId, String key, String value, AddCacheEntryResult<Long, String> addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder.cacheId(cacheId);
        entryCreatedEncoder.key(key);
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
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
    protected void handlePostRemoveCacheEntry(Long cacheId, String key, RemoveCacheEntryResult<Long, String> removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder.cacheId(cacheId);
        entryRemovedEncoder.key(key);
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
    protected void handlePostClearCache(Long cacheId, ClearCacheResult<Long> clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder.cacheId(cacheId);
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * After a cache is deleted, send out a CacheDeleted SBE message.
     *
     * @param cacheId           The ID of the cache which was deleted.
     * @param deleteCacheResult The deleted cache.
     * @param session           The client session.
     * @param buffer            The buffer from which the delete request was created.
     * @param offset            The offset from within the buffer to decode the original request from.
     */
    @Override
    protected void handlePostDeleteCache(Long cacheId, Cache<Long, String, String> deleteCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder.cacheId(cacheId);
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
    }
}
