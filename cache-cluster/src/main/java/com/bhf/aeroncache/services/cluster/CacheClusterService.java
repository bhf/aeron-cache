package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;
import java.lang.Long;


/**
 * The cache cluster service provides access to a CacheManager via an
 * Aeron cluster interface. It processes the core messages of the cache and
 * delegates those to the implementation of the
 * {@link CacheManager}.
 */
@Log4j2
@Builder
public class CacheClusterService implements ClusteredService {
    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CreateCacheDecoder createCacheDecoder = new CreateCacheDecoder();
    private final AddCacheEntryDecoder addCacheEntryDecoder = new AddCacheEntryDecoder();
    private final MutableDirectBuffer egressBuffer = new ExpandableArrayBuffer();
    private Cluster cluster;
    private IdleStrategy idleStrategy;
    private final CacheManagerFactory<Long, String, String> cacheManagerFactory=new CacheManagerFactory<>();
    private CacheManager<Long, String, String> cacheManager = cacheManagerFactory.getCacheManager(getSnapshotConsumer(), getImageConsumer());
    private Consumer<Image> imageConsumer;
    private Consumer<ExclusivePublication> snapshotConsumer;

    private Consumer<Image> getImageConsumer() {
        return imageConsumer;
    }

    private Consumer<ExclusivePublication> getSnapshotConsumer() {
        return snapshotConsumer;
    }

    private String nodeId;

    /**
     * Cluster started.
     *
     * @param cluster       with which the service can interact.
     * @param snapshotImage from which the service can load its archived state which can be null when no snapshot.
     */
    public void onStart(final Cluster cluster, final Image snapshotImage) {
        this.cluster = cluster;
        this.idleStrategy = cluster.idleStrategy();
        if (null != snapshotImage) {
            loadSnapshot(cluster, snapshotImage);
        }
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

    /**
     * Handle a cache clear request.
     *
     * @param session Session requesting the clear operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    private void handleClearCache(ClientSession session, DirectBuffer buffer, int offset) {
    }

    /**
     * @param session Session requesting the remove cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    private void handleRemoveCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        
    }

    /**
     * @param session Session requesting the add entry operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    private void handleAddCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        addCacheEntryDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = addCacheEntryDecoder.cacheName();
        var key = addCacheEntryDecoder.key();
        var value = addCacheEntryDecoder.entryValue();
        var addCacheEntryResult = cacheManager.getCache(cacheId).add(key, value);
    }

    /**
     * @param session Session requesting the create cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    private void handleCreateCache(ClientSession session, DirectBuffer buffer, int offset) {
        createCacheDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        long cacheId = createCacheDecoder.cacheName();
        log.info("Got create cache message for cache id {}", cacheId);
        var cacheCreationResult = cacheManager.createCache(cacheId);
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder.cacheName(cacheId);
        sendMessage(session, egressBuffer, cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength());
    }

    /**
     * @param session   Session to send the message too.
     * @param msgBuffer The buffer containing the message.
     * @param len       The length of the message.
     */
    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        while (session.offer(msgBuffer, 0, len) < 0) {
            idleStrategy.idle();
        }
    }

    /**
     * Record the state to a publication.
     *
     * @param snapshotPublication to which the state should be recorded.
     */
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication) {
        cacheManager.takeSnapshot(snapshotPublication);
    }

    /**
     * @param cluster       The cluster from which we are loading the snapshot.
     * @param snapshotImage The snapshot image.
     */
    private void loadSnapshot(final Cluster cluster, final Image snapshotImage) {

        cacheManager.loadSnapshot(snapshotImage);

        /*final MutableBoolean isAllDataLoaded = new MutableBoolean(false);
        final FragmentHandler fragmentHandler = (buffer, offset, length, header) -> isAllDataLoaded.set(true);

        while (!snapshotImage.isEndOfStream()) {
            final int fragmentsPolled = snapshotImage.poll(fragmentHandler, 1);

            if (isAllDataLoaded.value) {
                break;
            }

            idleStrategy.idle(fragmentsPolled);
        }*/
    }

    /**
     * @param newRole that the node has assumed.
     */
    public void onRoleChange(final Cluster.Role newRole) {
        log.info("Node {} has new role of {}", nodeId, newRole);
    }

    /**
     * @param cluster with which the service can interact.
     */
    public void onTerminate(final Cluster cluster) {
        log.info("Node {} is terminating", nodeId);
    }

    /**
     * @param session   for the client which have been opened.
     * @param timestamp at which the session was opened.
     */
    public void onSessionOpen(final ClientSession session, final long timestamp) {
        log.info("Client session open {} on node {}", session, nodeId);
    }

    /**
     * @param session     that has been closed.
     * @param timestamp   at which the session was closed.
     * @param closeReason the session was closed.
     */
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason) {
        log.info("Client session closed {} on node {}, close reason: {}", session, nodeId, closeReason);
    }

    /**
     * @param correlationId for the expired timer.
     * @param timestamp     at which the timer expired.
     */
    public void onTimerEvent(final long correlationId, final long timestamp) {
    }

}
