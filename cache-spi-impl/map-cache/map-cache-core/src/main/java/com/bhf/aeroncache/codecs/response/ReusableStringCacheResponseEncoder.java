package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;
import org.apache.logging.log4j.util.Strings;

import java.util.Comparator;

public class ReusableStringCacheResponseEncoder implements CacheResponseEncoder<ReusableString, ReusableString, ReusableString> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CacheCreatedEncoder cacheCreatedEncoder = new CacheCreatedEncoder();
    private final CacheEntryCreatedEncoder entryCreatedEncoder = new CacheEntryCreatedEncoder();
    private final CacheEntryUpdateEncoder entryUpdateEncoder = new CacheEntryUpdateEncoder();
    private final CacheEntryResultEncoder cacheEntryResultEncoder = new CacheEntryResultEncoder();
    private final AllCacheEntriesResultEncoder allCacheEntriesResultEncoder = new AllCacheEntriesResultEncoder();
    private final CacheEntryRemovedEncoder entryRemovedEncoder = new CacheEntryRemovedEncoder();
    private final CacheClearedEncoder cacheClearedEncoder = new CacheClearedEncoder();
    private final CacheDeletedEncoder cacheDeletedEncoder = new CacheDeletedEncoder();
    private final AllCacheStatsResultEncoder cacheStatsResultEncoder = new AllCacheStatsResultEncoder();
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
        entryUpdateEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
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
    public <VT extends Reusable> int encodeAllCacheEntriesResult(ReusableString cacheId, GetAllCacheEntriesResult<ReusableString, ReusableString, VT> getAllCacheEntriesResult, MutableDirectBuffer egressBuffer) {
        allCacheEntriesResultEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        allCacheEntriesResultEncoder
                .status(getOperationStatus(getAllCacheEntriesResult.getStatus()))
                .endOfBatch(BooleanType.T);

        var values = getAllCacheEntriesResult.getValues();
        int size = values.size();
        var itemsEncoder = allCacheEntriesResultEncoder.itemsCount(size);

        values.forEach((key, value) -> {
            itemsEncoder.next();
            itemsEncoder.key(key.value()).value(value.value().toString());
        });

        allCacheEntriesResultEncoder
                .requestId(getAllCacheEntriesResult.getRequestId())
                .cacheId(cacheId.value());

        return allCacheEntriesResultEncoder.encodedLength() + headerEncoder.encodedLength();
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
    public int encodeBulkOpsResponse(BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkCacheOpsResult, MutableDirectBuffer egressBuffer) {
        bulkOperationResponseEncoder.wrapAndApplyHeader(egressBuffer, 0, headerEncoder);
        var itemsEncoder = bulkOperationResponseEncoder.itemsCount(bulkCacheOpsResult.getOperations().size());

        for (int i = 0; i < bulkCacheOpsResult.getOperations().size(); i++) {
            itemsEncoder.next();
            var op = bulkCacheOpsResult.getOperations().get(i);
            itemsEncoder.operationStatus(OperationStatus.valueOf(op.getOperationStatus().toString()))
                    .requestId(op.getRequestId())
                    .cacheId(op.getCacheId().value())
                    .key(op.getKey().value())
                    .value(op.getValue().value());
        }

        bulkOperationResponseEncoder.requestId(bulkCacheOpsResult.getRequestId());
        return bulkOperationResponseEncoder.encodedLength() + headerEncoder.encodedLength();
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
