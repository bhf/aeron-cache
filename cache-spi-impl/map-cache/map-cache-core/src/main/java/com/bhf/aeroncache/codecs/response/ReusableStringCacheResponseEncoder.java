package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
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

public class ReusableStringCacheResponseEncoder implements CacheResponseEncoder<ReusableString, ReusableString, ReusableString> {

    private static final int RESPONSE_BATCH_SIZE = 100;

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryUpdateEncoder entryUpdateEncoder = new CacheEntryUpdateEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final CacheEntryPatchedEncoder cacheEntryPatchedEncoder = new CacheEntryPatchedEncoder();
    private final AllCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCacheEntriesResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheItemRemovalCancelledEncoder itemRemovalCancelledEncoder = new CacheItemRemovalCancelledEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final AllCacheStatsResultEncoder cacheStatsResultEncoder = new AllCacheStatsResultEncoder();
    private final AllTimersResultEncoder allTimersResultEncoder = new AllTimersResultEncoder();
    private final CacheSubscriptionResponseEncoder cacheSubscriptionResponseEncoder = new CacheSubscriptionResponseEncoder();
    private final CacheUnsubscribeResponseEncoder cacheUnsubscribeResponseEncoder = new CacheUnsubscribeResponseEncoder();
    private final BulkOperationResponseEncoder bulkOperationResponseEncoder = new BulkOperationResponseEncoder();

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
                .key(key.value())
                .requestId(addCacheEntryResult.getRequestId());

        return entryCreatedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> int encodeEntryUpdated(ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return encodeEntryUpdate(key, value, addCacheEntryResult, UpdateEventType.ADD_ITEM, egressBuffer);
    }

    @Override
    public <VT extends Reusable> int encodeEntryPatched(ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, MutableDirectBuffer egressBuffer) {
        return encodeEntryUpdate(key, value, addCacheEntryResult, UpdateEventType.PATCH_ITEM, egressBuffer);
    }

    private <VT extends Reusable> int encodeEntryUpdate(ReusableString key, VT value, AddCacheEntryResult<ReusableString, ReusableString> addCacheEntryResult, UpdateEventType eventType, MutableDirectBuffer egressBuffer) {
        entryUpdateEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryUpdateEncoder.eventType(eventType);
        entryUpdateEncoder.cacheId((String) addCacheEntryResult.getCacheId().value())
                .key(key.value())
                .value(value.value().toString())
                .requestId(addCacheEntryResult.getRequestId());

        return entryUpdateEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> int encodeCacheEntryResult(ReusableString cacheId, GetCacheEntryResult<ReusableString, ReusableString, VT> getCacheEntryResult, MutableDirectBuffer egressBuffer) {
        cacheEntryResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryResultEncoder
                .status(getOperationStatus(getCacheEntryResult.getStatus()))
                .cacheId(cacheId.value())
                .key(getCacheEntryResult.getEntryKey().value());

        if (getCacheEntryResult.getEntryValue() != null) {
            cacheEntryResultEncoder.value(getCacheEntryResult.getEntryValue().toString());
        } else {
            cacheEntryResultEncoder.value(Strings.EMPTY);
        }

        cacheEntryResultEncoder.requestId(getCacheEntryResult.getRequestId());

        return cacheEntryResultEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> int encodePatchValueResult(ReusableString cacheId, PatchValueResult<ReusableString, ReusableString, VT> patchValueResult, MutableDirectBuffer egressBuffer) {
        cacheEntryPatchedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        cacheEntryPatchedEncoder
                .status(getOperationStatus(patchValueResult.getStatus()))
                .cacheId(cacheId.value())
                .key(patchValueResult.getEntryKey().value());

        cacheEntryPatchedEncoder.requestId(patchValueResult.getRequestId());

        return cacheEntryPatchedEncoder.encodedLength() + headerEncoder.encodedLength();
    }

    @Override
    public <VT extends Reusable> void encodeAllCacheEntriesResult(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, VT> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer) {
        var entries = new ArrayList<>(getAllCacheEntriesResult.getValues().entrySet());
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
        allCacheEntriesResultEncoder
                .status(status)
                .endOfBatch(endOfBatch ? BooleanType.T : BooleanType.F);

        var itemsEncoder = allCacheEntriesResultEncoder.itemsCount(count);
        for (int j = 0; j < count; j++) {
            var entry = entries.get(fromIndex + j);
            itemsEncoder.next();
            itemsEncoder.key(entry.getKey().value()).value(entry.getValue().value().toString());
        }

        allCacheEntriesResultEncoder
                .requestId(requestId)
                .cacheId(cacheId);

        int length = allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength();
        consumer.setBuffer(egressBuffer);
        consumer.setLength(length);
        consumer.accept(egressBuffer);
    }

    @Override
    public int encodeRemoveCacheEntryResult(ReusableString cacheId, ReusableString key, RemoveCacheEntryResult<ReusableString, ReusableString> removeCacheEntryResult, MutableDirectBuffer egressBuffer) {
        entryRemovedEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        entryRemovedEncoder
                .status(getOperationStatus(removeCacheEntryResult.getStatus()))
                .cacheId(cacheId.value())
                .key(key.value())
                .requestId(removeCacheEntryResult.getRequestId());

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

    @Override
    public <VT extends Reusable> void encodeCacheSubscriptionResult(CacheSubscriptionResult<ReusableString, ReusableString, VT> subscriptionRequestResult,
                                                                   MutableDirectBuffer egressBuffer, Comparator<ReusableString> keyComparator, HydratingPublicationConsumer consumer) {
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
                var itemKey = sortedKeys.get(i + j).value();
                var itemValue = entries.get(itemKey).value();
                itemsEncoder.next();
                itemsEncoder.key(itemKey).value(itemValue.toString()).cacheId(cacheId);
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
    public void encodeBulkOpsResponse(BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkCacheOpsResult, MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer) {
        var operations = bulkCacheOpsResult.getOperations();
        var requestId = bulkCacheOpsResult.getRequestId();
        boolean isResultEob = bulkCacheOpsResult.isEndOfBatch();
        int totalSize = operations.size();

        if (totalSize == 0) {
            encodeBulkOpsBatch(egressBuffer, consumer, isResultEob, requestId, operations, 0, 0);
            return;
        }

        for (int i = 0; i < totalSize; i += RESPONSE_BATCH_SIZE) {
            int currentBatchSize = Math.min(RESPONSE_BATCH_SIZE, totalSize - i);
            boolean isLastBatch = (i + currentBatchSize) == totalSize;
            encodeBulkOpsBatch(egressBuffer, consumer, isLastBatch && isResultEob, requestId, operations, i, currentBatchSize);
        }
    }

    private void encodeBulkOpsBatch(MutableDirectBuffer egressBuffer, HydratingPublicationConsumer consumer, boolean endOfBatch, String requestId,
                                    List<CacheOperationResultDetails<ReusableString, ReusableString, ReusableString>> operations, int fromIndex, int count) {
        bulkOperationResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        bulkOperationResponseEncoder.endOfBatch(endOfBatch ? BooleanType.T : BooleanType.F);

        var itemsEncoder = bulkOperationResponseEncoder.itemsCount(count);
        for (int j = 0; j < count; j++) {
            var op = operations.get(fromIndex + j);
            itemsEncoder.next();
            itemsEncoder.operationStatus(OperationStatus.valueOf(op.getOperationStatus().toString()))
                    .requestId(op.getRequestId())
                    .cacheId(op.getCacheId().value())
                    .key(op.getKey().value())
                    .value(op.getValue().value());
        }

        bulkOperationResponseEncoder.requestId(requestId);

        int length = bulkOperationResponseEncoder.encodedLength() + headerEncoder.encodedLength();
        consumer.setBuffer(egressBuffer);
        consumer.setLength(length);
        consumer.accept(egressBuffer);
    }

    private OperationStatus getOperationStatus(CacheOperationStatus status) {
        switch(status){
            case CACHE_EXISTS -> {
                return OperationStatus.CACHE_EXISTS;
            }
            case DUPLICATE_SUBSCRIPTION -> {
                return OperationStatus.DUPLICATE_SUBSCRIPTION;
            }
            case UNKNOWN_CACHE -> {
                return OperationStatus.UNKNOWN_CACHE;
            }
            case UNKNOWN_KEY -> {
                return OperationStatus.UNKNOWN_KEY;
            }
            case UNKNOWN_SUBSCRIPTION -> {
                return OperationStatus.UNKNOWN_SUBSCRIPTION;
            }
            case SUCCESS -> {
                return OperationStatus.SUCCESS;
            }
            case NULL_VAL -> {
                return OperationStatus.NULL_VAL;
            }
            case NONE -> {
                return OperationStatus.NONE;
            }
            case ERROR -> {
                return OperationStatus.ERROR;
            }
        }

        return OperationStatus.NONE;
    }
}
