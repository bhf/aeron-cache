package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
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

class SBEDecodingCacheClusterService extends AbstractCacheClusterService<Long, String, String> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();

    SBEDecodingCacheClusterService(Cluster cluster, IdleStrategy idleStrategy, CacheManager<Long, String, String> cacheManager, Consumer<Image> imageConsumer, Consumer<ExclusivePublication> snapshotConsumer, String nodeId) {
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
    protected CreateCacheRequestDetails<Long> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheRequestDetails.clear();
        return createCacheRequestDetails;
    }

    @Override
    protected ClearCacheRequestDetails<Long> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        clearCacheRequestDetails.clear();
        return clearCacheRequestDetails;
    }

    @Override
    protected RemoveCacheEntryRequestDetails<Long, String> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset) {
        removeCacheEntryRequestDetails.clear();
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
    protected void handlePostCreateCache(Long cacheId, CreateCacheResult cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheName(cacheId);
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    @Override
    protected void handlePostAddCacheEntry(Long cacheId, String key, String value, AddCacheEntryResult addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {

    }

    @Override
    protected void handlePostRemoveCacheEntry(Long cacheId, String key, RemoveCacheEntryResult removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset) {

    }

    @Override
    protected void handlePostClearCache(Long cacheId, ClearCacheResult clearCacheResult, ClientSession session, DirectBuffer buffer, int offset) {

    }
}
