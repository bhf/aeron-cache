package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ReusableStringCountersCacheResponseEncoder implements CountersCacheResponseEncoder<ReusableString, ReusableString, ReusableLong> {

    private static final int RESPONSE_BATCH_SIZE = 100;

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCounterCacheResponseEncoder cacheCreatedEncoder = new CreateCounterCacheResponseEncoder();
    private final AddCounterResponseEncoder entryCreatedEncoder = new AddCounterResponseEncoder();
    private final CounterCacheEntryResultEncoder cacheEntryResultEncoder = new CounterCacheEntryResultEncoder();
    private final AllCounterCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCounterCacheEntriesResultEncoder();
    private final RemoveCounterResponseEncoder entryRemovedEncoder = new RemoveCounterResponseEncoder();
    private final CounterItemRemovalCancelledEncoder itemRemovalCancelledEncoder = new CounterItemRemovalCancelledEncoder();
    private final ClearCounterCacheResponseEncoder cacheClearedEncoder = new ClearCounterCacheResponseEncoder();
    private final DeleteCounterCacheResponseEncoder cacheDeletedEncoder = new DeleteCounterCacheResponseEncoder();
    private final CounterCacheSubscriptionResponseEncoder cacheSubscriptionResponseEncoder = new CounterCacheSubscriptionResponseEncoder();
    private final CounterCacheUnsubscribeResponseEncoder cacheUnsubscribeResponseEncoder = new CounterCacheUnsubscribeResponseEncoder();

    private final CounterCacheEntryUpdateEncoder entryUpdateEncoder = new CounterCacheEntryUpdateEncoder();
    private final IncrementCounterResponseEncoder incrementEncoder = new IncrementCounterResponseEncoder();
    private final DecrementCounterResponseEncoder decrementEncoder = new DecrementCounterResponseEncoder();
    private final SetCounterResponseEncoder setEncoder = new SetCounterResponseEncoder();
    private final AllCounterCacheStatsResultEncoder cacheStatsResultEncoder = new AllCounterCacheStatsResultEncoder();


    @Override
    public <VT extends Reusable> int encodePatchValueResult(ReusableString cacheId, PatchValueResult<ReusableString, ReusableString, VT> patchValueResult, MutableDirectBuffer egressBuffer) {
        throw new UnsupportedOperationException("Patch value is not supported for counter caches");
    }

    @Override
    public int encodeIncrementResult(ReusableString cacheId, IncrementCounterResult<ReusableString, ReusableString> result, MutableDirectBuffer egressBuffer) {
        incrementEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder)
                .counterValue(result.getCounterValue())
                .status(getOperationStatus(result.getStatus()));
        incrementEncoder.cacheId(cacheId.value());
        incrementEncoder.requestId(result.getRequestId());
        incrementEncoder.counterId(result.getKey().value());
        return incrementEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDecrementResult(ReusableString cacheId, DecrementCounterResult<ReusableString, ReusableString> result, MutableDirectBuffer egressBuffer) {
        decrementEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder)
                .counterValue(result.getCounterValue())
                .status(getOperationStatus(result.getStatus()));
        decrementEncoder.cacheId(cacheId.value());
        decrementEncoder.requestId(result.getRequestId());
        decrementEncoder.counterId(result.getKey().value());
        return decrementEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeSetCounterResult(ReusableString cacheId, SetCounterResult<ReusableString, ReusableString> result, MutableDirectBuffer egressBuffer) {
        setEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder)
                .counterValue(result.getCounterValue())
                .status(getOperationStatus(result.getStatus()));
        setEncoder.cacheId(cacheId.value());
        setEncoder.requestId(result.getRequestId());
        setEncoder.counterId(result.getKey().value());
        return setEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheCreationResult(ReusableString cacheId, CreateCacheResult<ReusableString> cacheCreationResult, MutableDirectBuffer egressBuffer) {
        cacheCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheCreatedEncoder
                .status(getOperationStatus(cacheCreationResult.getStatus()))
                .cacheId(cacheId.value())
                .requestId(cacheCreationResult.getRequestId());
        return cacheCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntryResult(ReusableString cacheId, ReusableString key, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryCreatedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryCreatedEncoder
                .status(getOperationStatus(addCacheEntryResult.getStatus()))
                .cacheId(cacheId.value())
                .requestId(addCacheEntryResult.getRequestId())
                .counterId(key.value());

        return entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> int encodeEntryUpdated(ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryUpdateEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryUpdateEncoder.value((Long) value.value());
        entryUpdateEncoder.eventType(UpdateEventType.ADD_ITEM);
        entryUpdateEncoder.cacheId(addCacheEntryResult.getCacheId().value())
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());

        return entryUpdateEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> int encodeCacheEntryResult(ReusableString cacheId, GetCacheEntryResult<ReusableString, ReusableString, VT> getCacheEntryResult, MutableDirectBuffer egressBuffer) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder.status(getOperationStatus(getCacheEntryResult.getStatus()));

        if (getCacheEntryResult.getStatus() == CacheOperationStatus.SUCCESS && getCacheEntryResult.getEntryValue() != null) {
            ReusableLong v = (ReusableLong) getCacheEntryResult.getEntryValue();
            cacheEntryResultEncoder.counterValue(v.value());
        } else {
            cacheEntryResultEncoder.counterValue(0L);
        }

        cacheEntryResultEncoder.cacheId(cacheId.value());
        cacheEntryResultEncoder.key(getCacheEntryResult.getEntryKey().value());
        cacheEntryResultEncoder.requestId(getCacheEntryResult.getRequestId());
        return cacheEntryResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> void encodeAllCacheEntriesResult(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, VT> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer) {
        var values = getAllCacheEntriesResult.getValues();
        var entries = values == null ? new ArrayList<Map.Entry<ReusableString, VT>>() : new ArrayList<>(values.entrySet());
        var status = getOperationStatus(getAllCacheEntriesResult.getStatus());
        var requestId = getAllCacheEntriesResult.getRequestId();
        var cacheIdValue = cacheId.value();
        boolean isResultEob = getAllCacheEntriesResult.isEndOfBatch();
        int totalSize = entries.size();

        if (totalSize == 0) {
            encodeEntriesBatch(egressBuffer, consumer, status, isResultEob, requestId, cacheIdValue, entries, 0, 0);
            return;
        }

        for (int i = 0; i < totalSize; i += RESPONSE_BATCH_SIZE) {
            int currentBatchSize = Math.min(RESPONSE_BATCH_SIZE, totalSize - i);
            boolean isLastBatch = (i + currentBatchSize) == totalSize;
            encodeEntriesBatch(egressBuffer, consumer, status, isLastBatch && isResultEob, requestId, cacheIdValue, entries, i, currentBatchSize);
        }
    }

    private <VT extends Reusable> void encodeEntriesBatch(MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer, OperationStatus status,
                                                          boolean endOfBatch, String requestId, String cacheId,
                                                          List<Map.Entry<ReusableString, VT>> entries, int fromIndex, int count) {
        allCacheEntriesResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allCacheEntriesResultEncoder.status(status);
        allCacheEntriesResultEncoder.endOfBatch(endOfBatch ? BooleanType.T : BooleanType.F);

        AllCounterCacheEntriesResultEncoder.ItemsEncoder entriesEncoder = allCacheEntriesResultEncoder.itemsCount(count);
        for (int j = 0; j < count; j++) {
            var entry = entries.get(fromIndex + j);
            entriesEncoder.next();
            entriesEncoder.counterValue(((ReusableLong) entry.getValue()).value());
            entriesEncoder.key(entry.getKey().value());
        }

        allCacheEntriesResultEncoder.requestId(requestId);
        allCacheEntriesResultEncoder.cacheId(cacheId);

        int length = allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength();
        consumer.setBuffer(egressBuffer);
        consumer.setLength(length);
        consumer.accept(egressBuffer);
    }

    @Override
    public int encodeRemoveCacheEntryResult(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder
                .counterValue(0)
                .status(getOperationStatus(removeCacheEntryResult.getStatus()))
                .cacheId(cacheId.value())
                .requestId(removeCacheEntryResult.getRequestId())
                .counterId(key.value());
        return entryRemovedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeItemRemovalCancelled(ReusableString cacheId, ReusableString key, CancelItemRemovalResult<ReusableString, ReusableString> cancelItemRemovalResult, MutableDirectBuffer egressBuffer) {
        itemRemovalCancelledEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        itemRemovalCancelledEncoder
                .status(getOperationStatus(cancelItemRemovalResult.getStatus()))
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(cancelItemRemovalResult.getRequestId());

        return itemRemovalCancelledEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheCleared(ReusableString cacheId, ClearCacheResult<ReusableString> clearCacheResult, MutableDirectBuffer egressBuffer) {
        cacheClearedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheClearedEncoder
                .status(getOperationStatus(clearCacheResult.getStatus()))
                .cacheId(cacheId.value())
                .requestId(clearCacheResult.getRequestId());
        return cacheClearedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(ReusableString cacheId, DeleteCacheResult<ReusableString> deleteCacheResult, MutableDirectBuffer egressBuffer) {
        cacheDeletedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheDeletedEncoder
                .status(getOperationStatus(deleteCacheResult.getStatus()))
                .cacheId(cacheId.value())
                .requestId(deleteCacheResult.getRequestId());
        return cacheDeletedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheStatsResult(CacheStatsResult<ReusableString> cacheStatsResult, MutableDirectBuffer egressBuffer) {
        cacheStatsResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheStatsResultEncoder.status(com.bhf.aeroncache.messages.OperationStatus.SUCCESS);

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

        return cacheStatsResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> void encodeCacheSubscriptionResult(CacheSubscriptionResult<ReusableString, ReusableString, VT> subscriptionRequestResult, MutableDirectBuffer egressBuffer, Comparator<ReusableString> keyComparator, HydratingPublicationConsumer consumer) {
        var entries = subscriptionRequestResult.entries;
        var cacheId = subscriptionRequestResult.getCacheId().value();
        var requestId = subscriptionRequestResult.getRequestId();
        var status = getOperationStatus(subscriptionRequestResult.getStatus());
        boolean isResultEob = subscriptionRequestResult.isEob();

        if (entries == null || entries.isEmpty()) {
            encodeNonHydratingCacheSubscriptionResult(egressBuffer, consumer, status, isResultEob, cacheId, requestId);
            return;
        }

        var sortedKeys = entries.keySet().stream().sorted(keyComparator).toList();
        int totalSize = sortedKeys.size();
        int batchSize = 100;
        int lastEncodedLength = 0;

        for (int i = 0; i < totalSize; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalSize - i);
            boolean isLastBatch = (i + currentBatchSize) == totalSize;

            cacheSubscriptionResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
            cacheSubscriptionResponseEncoder.status(status);

            cacheSubscriptionResponseEncoder.isEob((isLastBatch && isResultEob) ? BooleanType.T : BooleanType.F);

            var itemsEncoder = cacheSubscriptionResponseEncoder.itemsCount(currentBatchSize);

            for (int j = 0; j < currentBatchSize; j++) {
                var rsKey = sortedKeys.get(i + j);
                var itemKey = rsKey.value();
                var itemValue = entries.get(rsKey);
                ReusableLong v = (ReusableLong) itemValue;
                itemsEncoder.next();
                itemsEncoder.counterValue(v.value())
                        .counterId(itemKey)
                        .cacheId(cacheId);
            }

            cacheSubscriptionResponseEncoder.cacheId(cacheId)
                    .requestId(requestId);

            lastEncodedLength = cacheSubscriptionResponseEncoder.encodedLength() + headerEncoder.encodedLength();

            consumer.setBuffer(egressBuffer);
            consumer.setLength(lastEncodedLength);
            consumer.accept(egressBuffer);
        }
    }

    private void encodeNonHydratingCacheSubscriptionResult(MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer, OperationStatus status, boolean isResultEob, String cacheId, String requestId) {
        cacheSubscriptionResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);

        cacheSubscriptionResponseEncoder.status(status)
                .isEob(isResultEob ? BooleanType.T : BooleanType.F)
                .itemsCount(0);

        cacheSubscriptionResponseEncoder.cacheId(cacheId).requestId(requestId);

        int length = cacheSubscriptionResponseEncoder.encodedLength() + headerEncoder.encodedLength();

        consumer.setBuffer(egressBuffer);
        consumer.setLength(length);
        consumer.accept(egressBuffer);
    }

    @Override
    public int encodeCacheUnsubscribeResponse(CacheUnsubscribeResult<ReusableString> unsubscribeResponse, MutableDirectBuffer egressBuffer) {
        cacheUnsubscribeResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheUnsubscribeResponseEncoder
                .status(getOperationStatus(unsubscribeResponse.getStatus()))
                .cacheId(unsubscribeResponse.getCacheId().value())
                .requestId(unsubscribeResponse.getRequestId());
        return cacheUnsubscribeResponseEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public void encodeBulkOpsResponse(BulkCacheOpsResult<ReusableString, ReusableString, ReusableLong> bulkCacheOpsResult, MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer) {
        // Bulk responses are always encoded via the regular-cache encoder; counter bulk ops are folded into
        // that single response, so there is nothing to encode here.
    }


    private com.bhf.aeroncache.messages.OperationStatus getOperationStatus(com.bhf.aeroncache.models.results.CacheOperationStatus status) {
        if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.SUCCESS) {
            return com.bhf.aeroncache.messages.OperationStatus.SUCCESS;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.ERROR) {
            return com.bhf.aeroncache.messages.OperationStatus.ERROR;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.UNKNOWN_KEY) {
            return com.bhf.aeroncache.messages.OperationStatus.UNKNOWN_KEY;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.UNKNOWN_CACHE) {
            return com.bhf.aeroncache.messages.OperationStatus.UNKNOWN_CACHE;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.CACHE_EXISTS) {
            return com.bhf.aeroncache.messages.OperationStatus.CACHE_EXISTS;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.DUPLICATE_SUBSCRIPTION) {
            return com.bhf.aeroncache.messages.OperationStatus.DUPLICATE_SUBSCRIPTION;
        } else if (status == com.bhf.aeroncache.models.results.CacheOperationStatus.UNKNOWN_SUBSCRIPTION) {
            return com.bhf.aeroncache.messages.OperationStatus.UNKNOWN_SUBSCRIPTION;
        }
        return com.bhf.aeroncache.messages.OperationStatus.ERROR;
    }

    private final AllTimersResultEncoder allTimersResultEncoder = new AllTimersResultEncoder();

    @Override
    public void encodeAllTimersResult(AllTimersResult<ReusableString, ReusableString> allTimersResult, MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer) {
        var timers = allTimersResult.getTimers();
        var status = getOperationStatus(allTimersResult.getOperationStatus());
        var requestId = allTimersResult.getRequestId();
        boolean isResultEob = allTimersResult.isEndOfBatch();
        int totalSize = timers.size();
        int batchSize = 100;

        if (totalSize == 0) {
            encodeTimersBatch(egressBuffer, consumer, status, isResultEob, requestId, timers, 0, 0);
            return;
        }

        for (int i = 0; i < totalSize; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalSize - i);
            boolean isLastBatch = (i + currentBatchSize) == totalSize;
            encodeTimersBatch(egressBuffer, consumer, status, isLastBatch && isResultEob, requestId, timers, i, currentBatchSize);
        }
    }

    private void encodeTimersBatch(MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer, OperationStatus status,
                                   boolean endOfBatch, String requestId, java.util.List<com.bhf.aeroncache.models.results.TimerDetails<ReusableString, ReusableString>> timers,
                                   int fromIndex, int count) {
        allTimersResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allTimersResultEncoder.status(status);
        allTimersResultEncoder.endOfBatch(endOfBatch ? BooleanType.T : BooleanType.F);

        var itemsEncoder = allTimersResultEncoder.timersCount(count);
        for (int j = 0; j < count; j++) {
            var t = timers.get(fromIndex + j);
            itemsEncoder.next();
            itemsEncoder.timerType(getTimerType(t.timerType));
            itemsEncoder.deadline(t.deadline);
            itemsEncoder.cacheId(t.getCacheId().value());
            itemsEncoder.key(t.getKey().value());
        }

        allTimersResultEncoder.requestId(requestId);

        int length = allTimersResultEncoder.encodedLength() + headerEncoder.encodedLength();
        consumer.setBuffer(egressBuffer);
        consumer.setLength(length);
        consumer.accept(egressBuffer);
    }

    private static com.bhf.aeroncache.messages.TimerType getTimerType(com.bhf.aeroncache.models.results.TimerType timerType) {
        return timerType == com.bhf.aeroncache.models.results.TimerType.COUNTER
                ? com.bhf.aeroncache.messages.TimerType.COUNTER
                : com.bhf.aeroncache.messages.TimerType.CACHE;
    }
}
