package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.ClearCacheResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.RemoveCacheEntryResult;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.requests.ClearCacheRequestDetails;
import com.bhf.aeroncache.models.requests.CreateCacheRequestDetails;
import com.bhf.aeroncache.models.requests.RemoveCacheEntryRequestDetails;
import com.bhf.aeroncache.services.cachemanager.CacheManager;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.Consumer;


/**
 * The cache cluster service provides access to a CacheManager via an
 * Aeron cluster interface. It processes the core messages of the cache and
 * delegates those to the implementation of the
 * {@link CacheManager}.
 */
@Log4j2
public abstract class AbstractCacheClusterService<I, K, V> implements ClusteredService {
    private Cluster cluster;
    private IdleStrategy idleStrategy;
    private final CacheManagerFactory<I, K, V> cacheManagerFactory = new CacheManagerFactory<>();
    private CacheManager<I, K, V> cacheManager = cacheManagerFactory.getCacheManager(getSnapshotConsumer(), getImageConsumer());
    private Consumer<Image> imageConsumer;
    private Consumer<ExclusivePublication> snapshotConsumer;
    final CreateCacheRequestDetails<I> createCacheRequestDetails = new CreateCacheRequestDetails<>();
    final ClearCacheRequestDetails<I> clearCacheRequestDetails = new ClearCacheRequestDetails<>();
    final RemoveCacheEntryRequestDetails<I, K> removeCacheEntryRequestDetails = new RemoveCacheEntryRequestDetails<>();
    final AddCacheEntryRequestDetails<I, K, V> addCacheEntryRequestDetails = new AddCacheEntryRequestDetails<>();

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
        log.info("On start called on cluster service");
        this.cluster = cluster;
        this.idleStrategy = cluster.idleStrategy();
        if (null != snapshotImage) {
            loadSnapshot(cluster, snapshotImage);
        }
    }

    /**
     * Handle a cache clear request.
     *
     * @param session Session requesting the clear operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleClearCache(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getClearCacheRequestDetails(session, buffer, offset);
        I cacheId = requestDetails.getCacheId();
        var clearCacheResult = cacheManager.clearCache(cacheId);
        handlePostClearCache(cacheId, clearCacheResult, session, buffer, offset);
    }

    /**
     * @param session Session requesting the remove cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleRemoveCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails = getRemoveCacheEntryRequestDetails(session, buffer, offset);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        var removeCacheEntryResult = cacheManager.getCache(cacheId).remove(key);
        handlePostRemoveCacheEntry(cacheId, key, removeCacheEntryResult, session, buffer, offset);
    }

    /**
     * @param session Session requesting the add entry operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleAddCacheEntry(ClientSession session, DirectBuffer buffer, int offset) {
        var requestDetails  = getAddCacheEntryRequestDetails(session, buffer, offset);
        I cacheId = requestDetails.getCacheId();
        K key = requestDetails.getKey();
        V value = requestDetails.getValue();
        var addCacheEntryResult = cacheManager.getCache(cacheId).add(key, value);
        handlePostAddCacheEntry(cacheId, key, value, addCacheEntryResult, session, buffer, offset);
    }


    /**
     * @param session Session requesting the create cache operation.
     * @param buffer  Buffer containing the message.
     * @param offset  Offset in the buffer at which the message is encoded.
     */
    void handleCreateCache(ClientSession session, DirectBuffer buffer, int offset) {
        CreateCacheRequestDetails<I> requestDetails = getCreateCacheRequestDetails(session, buffer, offset);
        I cacheId = requestDetails.getCacheId();
        log.info("Got create cache message for cache id {}", cacheId);
        var cacheCreationResult = cacheManager.createCache(cacheId);
        handlePostCreateCache(cacheId, cacheCreationResult, session, buffer, offset);
    }

    protected abstract CreateCacheRequestDetails<I> getCreateCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset);
    protected abstract ClearCacheRequestDetails<I> getClearCacheRequestDetails(ClientSession session, DirectBuffer buffer, int offset);
    protected abstract RemoveCacheEntryRequestDetails<I,K> getRemoveCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset);
    protected abstract AddCacheEntryRequestDetails<I,K,V> getAddCacheEntryRequestDetails(ClientSession session, DirectBuffer buffer, int offset);
    protected abstract void handlePostCreateCache(I cacheId, CreateCacheResult cacheCreationResult, ClientSession session, DirectBuffer buffer, int offset);
    protected abstract void handlePostAddCacheEntry(I cacheId, K key, V value, AddCacheEntryResult addCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);
    protected abstract void handlePostRemoveCacheEntry(I cacheId, K key, RemoveCacheEntryResult removeCacheEntryResult, ClientSession session, DirectBuffer buffer, int offset);
    protected abstract void handlePostClearCache(I cacheId, ClearCacheResult clearCacheResult, ClientSession session, DirectBuffer buffer, int offset);



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
