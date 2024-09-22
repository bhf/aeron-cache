package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.AddCacheEntryResult;
import com.bhf.aeroncache.models.CacheClearResult;
import com.bhf.aeroncache.models.CacheCreationResult;
import com.bhf.aeroncache.models.RemoveCacheEntryResult;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.requests.ClearCacheRequestDetails;
import com.bhf.aeroncache.models.requests.CreateCacheRequestDetails;
import com.bhf.aeroncache.models.requests.RemoveCacheEntryRequestDetails;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;

class SBEDecodingCacheClusterService<I,K,V> extends AbstractCacheClusterService<I,K,V>{

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();
    private final CreateCacheRequestDetails<I> createCacheRequestDetails=new CreateCacheRequestDetails<I>();
    private final ClearCacheRequestDetails<I> clearCacheRequestDetails=new ClearCacheRequestDetails<I>();
    private final RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails=new RemoveCacheEntryRequestDetails<>();
    private final AddCacheEntryRequestDetails<I, K, V> addCacheEntryRequestDetails=new AddCacheEntryRequestDetails<>();

    SBEDecodingCacheClusterService(Cluster cluster, IdleStrategy idleStrategy, CacheManager<I, K, V> cacheManager, Consumer<Image> imageConsumer, Consumer<ExclusivePublication> snapshotConsumer, String nodeId) {
        super(cluster, idleStrategy, cacheManager, imageConsumer, snapshotConsumer, nodeId);
    }

    /**
     * Process messages from cache clients.
     *
     * @param session   for the client which sent the message. This can be null if the client was a service.
     * @param timestamp for when the message was received.
     * @param buffer    containing the message.
     * @param offset    in the buffer at which the message is encoded.
     * @param length    of the encoded message.
     * @param header    aeron header for the incoming message.
     */
    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer, final int offset, final int length, final Header header) {
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();

        switch (templateId) {
            case CreateCacheEncoder.TEMPLATE_ID -> handleCreateCache(session, buffer, offset);
            case AddCacheEntryEncoder.TEMPLATE_ID -> handleAddCacheEntry(session, buffer, offset);
            case RemoveCacheEntryEncoder.TEMPLATE_ID -> handleRemoveCacheEntry(session, buffer, offset);
            case ClearCacheEncoder.TEMPLATE_ID -> handleClearCache(session, buffer, offset);
            default -> throw new IllegalStateException("Unexpected value: " + templateId);
        }
    }

    @Override
    protected CreateCacheRequestDetails<I> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<I> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<I, K> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
        return removeCacheEntryRequestDetails;
    }

    @Override
    protected AddCacheEntryRequestDetails<I, K, V> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryRequestDetails.clear();
        return addCacheEntryRequestDetails;
    }

    @Override
    protected void handlePostCreateCache(I cacheId, CacheCreationResult cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {

    }

    @Override
    protected void handlePostAddCacheEntry(I cacheId, K key, V value, AddCacheEntryResult addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {

    }

    @Override
    protected void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {

    }

    @Override
    protected void handlePostClearCache(I cacheId, CacheClearResult clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {

    }
}
