package com.bhf.aeroncache.services.tracing.impl;

import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.aeron.Aeron;
import io.aeron.Counter;
import lombok.extern.log4j.Log4j2;

/**
 * A tracing service that counts the requests made to Aeron Cache using counters in the Aeron media driver.
 * <p>
 * One counter is registered per operation and is incremented in the corresponding start* method only. The end*
 * methods do nothing. The counters can be viewed with tools that read the media driver's counters file
 * (e.g. AeronStat) or are exposed by the media driver counters export.
 * <p>
 * When constructed with an Aeron directory name, the Aeron client is connected lazily on the first request, since
 * the media driver may not have been launched when the cache service is created.
 */
@Log4j2
public class AeronCounterTracingService implements CacheTracingService {
    /**
     * Type id of the counters registered by this service. Values below 1000 are reserved by Aeron.
     */
    public static final int COUNTER_TYPE_ID = 2000;

    private enum Operation {
        DELETE_CACHE("DeleteCache"),
        CLEAR_CACHE("ClearCache"),
        REMOVE_CACHE_ENTRY("RemoveCacheEntry"),
        ADD_CACHE_ENTRY("AddCacheEntry"),
        GET_CACHE_ENTRY("GetCacheEntry"),
        GET_ALL_CACHE_ENTRIES("GetAllCacheEntries"),
        CREATE_CACHE("CreateCache"),
        GET_ALL_STATS("GetAllStats"),
        CACHE_SUBSCRIPTION("CacheSubscription"),
        CACHE_UNSUBSCRIBE("CacheUnsubscribe"),
        BULK_OPS("BulkOps"),
        PATCH_VALUE("PatchValue"),
        CANCEL_ITEM_REMOVAL("CancelItemRemoval"),
        GET_ALL_TIMERS("GetAllTimers"),
        INCREMENT_COUNTER("IncrementCounter"),
        DECREMENT_COUNTER("DecrementCounter"),
        SET_COUNTER("SetCounter");

        private final String label;

        Operation(String label) {
            this.label = label;
        }
    }

    private final String nodeId;
    private final String aeronDirectoryName;
    private final boolean ownsAeron;
    private Aeron aeron;
    private Counter[] counters;
    private boolean initialisationFailed;

    /**
     * Create a service that connects to the media driver in the given directory on the first request.
     *
     * @param aeronDirectoryName directory of the media driver.
     * @param nodeId             id of the node, included in the counter labels.
     */
    public AeronCounterTracingService(String aeronDirectoryName, String nodeId) {
        this.aeronDirectoryName = aeronDirectoryName;
        this.nodeId = nodeId;
        this.ownsAeron = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    /**
     * Create a service that registers its counters with an existing Aeron client. The client is not closed by
     * this service.
     *
     * @param aeron  client connected to the media driver.
     * @param nodeId id of the node, included in the counter labels.
     */
    public AeronCounterTracingService(Aeron aeron, String nodeId) {
        this.aeron = aeron;
        this.aeronDirectoryName = null;
        this.nodeId = nodeId;
        this.ownsAeron = false;
        registerCounters();
    }

    private void increment(Operation operation) {
        if (counters == null) {
            if (initialisationFailed) {
                return;
            }
            try {
                if (aeron == null) {
                    aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(aeronDirectoryName));
                }
                registerCounters();
            } catch (Exception e) {
                initialisationFailed = true;
                log.error("Could not register Aeron tracing counters, request counting is disabled", e);
                return;
            }
        }
        counters[operation.ordinal()].incrementOrdered();
    }

    private void registerCounters() {
        var registered = new Counter[Operation.values().length];
        for (Operation operation : Operation.values()) {
            registered[operation.ordinal()] =
                    aeron.addCounter(COUNTER_TYPE_ID, "AeronCache-Requests-" + operation.label + "-node" + nodeId);
        }
        counters = registered;
        log.info("Registered {} Aeron tracing counters for node {}", registered.length, nodeId);
    }

    private void close() {
        if (ownsAeron && aeron != null) {
            aeron.close();
        }
    }

    @Override
    public void startHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {
        increment(Operation.DELETE_CACHE);
    }

    @Override
    public void startHandleClearCache(ClearCacheRequestDetails requestDetails) {
        increment(Operation.CLEAR_CACHE);
    }

    @Override
    public void startRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {
        increment(Operation.REMOVE_CACHE_ENTRY);
    }

    @Override
    public void startAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {
        increment(Operation.ADD_CACHE_ENTRY);
    }

    @Override
    public void startGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {
        increment(Operation.GET_CACHE_ENTRY);
    }

    @Override
    public void startGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {
        increment(Operation.GET_ALL_CACHE_ENTRIES);
    }

    @Override
    public void startCreateCacheRequest(CreateCacheRequestDetails requestDetails) {
        increment(Operation.CREATE_CACHE);
    }

    @Override
    public void startGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {
        increment(Operation.GET_ALL_STATS);
    }

    @Override
    public void startCacheSubscriptionRequest(CacheSubscriptionRequestDetails requestDetails) {
        increment(Operation.CACHE_SUBSCRIPTION);
    }

    @Override
    public void startCacheUnsubscribeRequest(CacheUnsubscribeRequestDetails requestDetails) {
        increment(Operation.CACHE_UNSUBSCRIBE);
    }

    @Override
    public void startBulkOpsRequest(BulkCacheOpsRequestDetails requestDetails) {
        increment(Operation.BULK_OPS);
    }

    @Override
    public void startPatchValue(PatchValueRequestDetails requestDetails) {
        increment(Operation.PATCH_VALUE);
    }

    @Override
    public void startCancelItemRemoval(CancelItemRemovalRequestDetails requestDetails) {
        increment(Operation.CANCEL_ITEM_REMOVAL);
    }

    @Override
    public void startGetAllTimersRequest(GetAllTimersRequestDetails requestDetails) {
        increment(Operation.GET_ALL_TIMERS);
    }

    @Override
    public void startIncrementCounterRequest(IncrementCounterRequestDetails requestDetails) {
        increment(Operation.INCREMENT_COUNTER);
    }

    @Override
    public void startDecrementCounterRequest(DecrementCounterRequestDetails requestDetails) {
        increment(Operation.DECREMENT_COUNTER);
    }

    @Override
    public void startSetCounterRequest(SetCounterRequestDetails requestDetails) {
        increment(Operation.SET_COUNTER);
    }

    @Override
    public void endHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {

    }

    @Override
    public void endHandleClearCache(ClearCacheRequestDetails requestDetails) {

    }

    @Override
    public void endRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {

    }

    @Override
    public void endGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {

    }

    @Override
    public void endCreateCacheRequest(CreateCacheRequestDetails requestDetails) {

    }

    @Override
    public void endGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {

    }

    @Override
    public void endCacheSubscriptionRequest(CacheSubscriptionRequestDetails requestDetails) {

    }

    @Override
    public void endCacheUnsubscribeRequest(CacheUnsubscribeRequestDetails requestDetails) {

    }

    @Override
    public void endBulkOpsRequest(BulkCacheOpsRequestDetails requestDetails) {

    }

    @Override
    public void endPatchValue(PatchValueRequestDetails requestDetails) {

    }

    @Override
    public void endCancelItemRemoval(CancelItemRemovalRequestDetails requestDetails) {

    }

    @Override
    public void endGetAllTimersRequest(GetAllTimersRequestDetails requestDetails) {

    }

    @Override
    public void endIncrementCounterRequest(IncrementCounterRequestDetails requestDetails) {

    }

    @Override
    public void endDecrementCounterRequest(DecrementCounterRequestDetails requestDetails) {

    }

    @Override
    public void endSetCounterRequest(SetCounterRequestDetails requestDetails) {

    }
}
