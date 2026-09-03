package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.messages.*;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;

import java.util.List;

/**
 * Encode requests going to Aeron Cache.
 */
public class ReusableStringCacheRequestEncoder implements CacheRequestEncoder<ReusableString, ReusableString, ReusableString> {

    private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    private final CreateCacheEncoder createCacheEncoder = new CreateCacheEncoder();
    private final AddCacheEntryEncoder addCacheEntryEncoder = new AddCacheEntryEncoder();
    private final GetCacheEntryEncoder getCacheEntryEncoder = new GetCacheEntryEncoder();
    private final ClearCacheEncoder clearCacheEncoder = new ClearCacheEncoder();
    private final DeleteCacheEncoder deleteCacheEncoder = new DeleteCacheEncoder();
    private final RemoveCacheEntryEncoder removeCacheEntryEncoder = new RemoveCacheEntryEncoder();
    private final GetAllCacheEntriesEncoder getAllCacheEntriesEncoder = new GetAllCacheEntriesEncoder();
    private final CacheSubscriptionRequestEncoder cacheSubscriptionRequestEncoder = new CacheSubscriptionRequestEncoder();
    private final CacheUnsubscribeRequestEncoder cacheUnsubscribeRequestEncoder = new CacheUnsubscribeRequestEncoder();
    private final GetCacheStatsEncoder getCacheStatsEncoder = new GetCacheStatsEncoder();
    private final BulkOperationRequestEncoder bulkOpsEncoder = new BulkOperationRequestEncoder();

    @Override
    public int encodeCreateCacheRequest(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        createCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value())
                .requestId(requestId);
        return createCacheEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeAddCacheEntry(String requestId, ReusableString cacheId, ReusableString key, ReusableString value, long ttl, MutableDirectBuffer msgBuffer) {
        addCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .ttl(ttl)
                .cacheId(cacheId.value())
                .requestId(requestId)
                .key(key.value())
                .entryValue(value.value());
        return addCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        getCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).key(key.value()).requestId(requestId);
        return getCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeClearCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        clearCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).requestId(requestId);
        return clearCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeDeleteCache(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        deleteCacheEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).requestId(requestId);
        return deleteCacheEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeRemoveCacheEntry(String requestId, ReusableString cacheId, ReusableString key, MutableDirectBuffer msgBuffer) {
        removeCacheEntryEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).key(key.value()).requestId(requestId);
        return removeCacheEntryEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetCacheEntries(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        getAllCacheEntriesEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).requestId(requestId);
        return getAllCacheEntriesEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheSubscribe(String requestId, List<ReusableString> cacheId, boolean sendSnapshot, MutableDirectBuffer msgBuffer) {
        cacheSubscriptionRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .sendSnapshot(sendSnapshot ? BooleanType.T : BooleanType.F);

        var itemsEncoder = cacheSubscriptionRequestEncoder.cacheIdsCount(cacheId.size());

        for (int i = 0; i < cacheId.size(); i++) {
            itemsEncoder.next();
            itemsEncoder.cacheId(cacheId.get(i).value());
        }

        cacheSubscriptionRequestEncoder.requestId(requestId);
        return cacheSubscriptionRequestEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeCacheUnsubscribe(String requestId, ReusableString cacheId, MutableDirectBuffer msgBuffer) {
        cacheUnsubscribeRequestEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId.value()).requestId(requestId);
        return cacheUnsubscribeRequestEncoder.encodedLength()+headerEncoder.encodedLength();
    }

    @Override
    public int encodeGetAllCacheStats(String requestId, MutableDirectBuffer msgBuffer) {
        getCacheStatsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .requestId(requestId);
        return getCacheStatsEncoder.encodedLength()+ headerEncoder.encodedLength();
    }

    @Override
    public int encodeBulkOperations(String requestId, BulkCacheOpsRequest request, MutableDirectBuffer msgBuffer) {
        bulkOpsEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        var itemsEncoder = bulkOpsEncoder.itemsCount(request.operations().size());

        for (int i = 0; i < request.operations().size(); i++) {
            itemsEncoder.next();
            var op = request.operations().get(i);
            BulkOperationType opType = BulkOperationType.valueOf(op.operationType().toString());
            itemsEncoder.operationType(opType)
                    .ttl(op.ttl())
                    .counterValue(op.counterValue())
                    .requestId(op.requestId())
                    .cacheId(op.cacheId())
                    .key(op.key())
                    .value(op.value());
        }


        bulkOpsEncoder.requestId(requestId);
        return bulkOpsEncoder.encodedLength()+ headerEncoder.encodedLength();
    }
}
