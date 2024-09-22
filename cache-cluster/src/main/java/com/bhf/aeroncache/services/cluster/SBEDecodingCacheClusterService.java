package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.services.cache.Cache;
import io.aeron.cluster.service.ClientSession;
import io.aeron.logbuffer.Header;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

@Log4j2
public class SBEDecodingCacheClusterService extends AbstractCacheClusterService<Long, String, String> {

    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final ClearCacheDecoder clearCacheDecoder = new ClearCacheDecoder();
    private final RemoveCacheEntryDecoder removeCacheEntryDecoder = new RemoveCacheEntryDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder=new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder=new CacheClearedEncoder();
    private final DeleteCacheDecoder deleteCacheDecoder=new DeleteCacheDecoder();
    private final CacheDeletedEncoder cacheDeletedEncoder=new CacheDeletedEncoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();


    @Override
    protected CreateCacheRequestDetails<Long> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = createCacheDecoder.cacheName();
        createCacheRequestDetails.setCacheId(cacheId);
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<Long> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        clearCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = clearCacheDecoder.cacheName();
        clearCacheRequestDetails.setCacheId(cacheId);
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<Long, String> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
        removeCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = removeCacheEntryDecoder.cacheName();
        var key = removeCacheEntryDecoder.key();
        removeCacheEntryRequestDetails.setCacheId(cacheId);
        removeCacheEntryRequestDetails.setKey(key);
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<Long, String, String> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryRequestDetails.clear();
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = addCacheEntryDecoder.cacheName();
        var key = addCacheEntryDecoder.key();
        var value = addCacheEntryDecoder.entryValue();
        addCacheEntryRequestDetails.setCacheId(cacheId);
        addCacheEntryRequestDetails.setKey(key);
        addCacheEntryRequestDetails.setValue(value);
        return addCacheEntryRequestDetails;
    }

    @Override
    protected DeleteCacheRequestDetails<Long> getDeleteCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        deleteCacheRequestDetails.clear();
        deleteCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = deleteCacheDecoder.cacheName();
        deleteCacheRequestDetails.setCacheId(cacheId);
        return deleteCacheRequestDetails;
    }

    @Override
    protected void handlePostCreateCache(Long cacheId, CreateCacheResult cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheName(cacheId);
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostAddCacheEntry(Long cacheId, String key, String value, AddCacheEntryResult addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder.cacheName(cacheId);
        entryCreatedEncoder.key(key);
        sendMessage(session, egressBuffer, entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostRemoveCacheEntry(Long cacheId, String key, RemoveCacheEntryResult removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder.cacheName(cacheId);
        entryRemovedEncoder.key(key);
        sendMessage(session, egressBuffer, entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostClearCache(Long cacheId, ClearCacheResult clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder.cacheName(cacheId);
        sendMessage(session, egressBuffer, cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostDeleteCache(Long cacheId, Cache<String, String> deleteCacheResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder.cacheName(cacheId);
        sendMessage(session, egressBuffer, cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength());
    }
}
