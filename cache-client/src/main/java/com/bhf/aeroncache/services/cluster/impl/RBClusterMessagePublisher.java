package com.bhf.aeroncache.services.cluster.impl;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.messages.*;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

/**
 * Publish various pre-encoded cache requests onto a {@link org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer} to
 * be processed by an {@link org.agrona.concurrent.Agent} in an {@link org.agrona.concurrent.AgentRunner}.
 */


public class RBClusterMessagePublisher extends ClusterMessagePublisher {


    final ManyToOneRingBuffer rb;

    public RBClusterMessagePublisher(AeronCache cluster, ManyToOneRingBuffer rb) {
        super(cluster);
        this.rb = rb;
    }

    @Override
    void publishAddCachEntry(AddCacheEntryEncoder addCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = addCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(addCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, addCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    public void publishCreateCache(CreateCacheEncoder createCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = createCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(createCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, createCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishGetCacheEntry(GetCacheEntryEncoder getCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = getCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(getCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, getCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishClearCache(ClearCacheEncoder clearCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = clearCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(clearCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, clearCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishDeleteCache(DeleteCacheEncoder deleteCache, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = deleteCache.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(deleteCache.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, deleteCache.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishRemoveCacheEntry(RemoveCacheEntryEncoder removeCacheEntry, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = removeCacheEntry.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(removeCacheEntry.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, removeCacheEntry.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishGetAllCacheStats(GetCacheStatsEncoder getCacheStats, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = getCacheStats.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(getCacheStats.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, getCacheStats.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishCacheUnsubscribe(CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = cacheUnsubscribeRequestEncoder.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(cacheUnsubscribeRequestEncoder.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, cacheUnsubscribeRequestEncoder.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishCacheSubscribe(CacheSubscriptionRequestEncoder cacheSubscriptionRequest, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = cacheSubscriptionRequest.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(cacheSubscriptionRequest.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, cacheSubscriptionRequest.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

    @Override
    void publishGetAllCacheEntries(GetAllCacheEntriesEncoder getAllCacheEntriesEncoder, MessageHeaderEncoder header, MutableDirectBuffer msgBuffer, int msgBufferOffset) {
        var msgLength = getAllCacheEntriesEncoder.encodedLength() + header.encodedLength();
        var index = rb.tryClaim(getAllCacheEntriesEncoder.sbeTemplateId(), msgLength);
        var destBuffer = rb.buffer();
        destBuffer.putBytes(index, getAllCacheEntriesEncoder.buffer(), msgBufferOffset, msgLength);
        rb.commit(index);
    }

}
