package com.bhf.aeroncache.codecs.fixtures;

import com.bhf.aeroncache.codecs.request.CacheRequestEncoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOpsRequest;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RequestEncoderFixture} backed by {@link ReusableStringCacheRequestEncoder}, which encodes from
 * {@link ReusableString} inputs.
 */
public class ReusableStringRequestEncoderFixture implements RequestEncoderFixture {

    private static final String REQUEST_ID = "jmh-request-id";
    private static final String CACHE_ID = "jmh-codec-cache";

    private CacheRequestEncoder<ReusableString, ReusableString, ReusableString> encoder;

    private ReusableString cacheId;
    private ReusableString key;
    private ReusableString value;

    private List<ReusableString> subscribeCacheIds;
    private List<ReusableString> subscribeKeys;
    private List<SubscriptionMode> subscribeModes;

    private BulkOpsRequest bulkRequest;

    @Override
    public void init(int groupItems, int valueLength) {
        encoder = new ReusableStringCacheRequestEncoder();

        cacheId = new ReusableString();
        cacheId.copyFrom(CACHE_ID);
        key = new ReusableString();
        key.copyFrom("jmh-key");
        value = new ReusableString();
        value.copyFrom("v".repeat(valueLength));

        subscribeCacheIds = new ArrayList<>(groupItems);
        subscribeKeys = new ArrayList<>(groupItems);
        subscribeModes = new ArrayList<>(groupItems);
        for (int i = 0; i < groupItems; i++) {
            var id = new ReusableString();
            id.copyFrom("cache-" + i);
            subscribeCacheIds.add(id);
            subscribeKeys.add(null); // whole-cache subscription
            subscribeModes.add(SubscriptionMode.FULL);
        }

        var ops = new ArrayList<CacheOperationRequest>(groupItems);
        for (int i = 0; i < groupItems; i++) {
            ops.add(new CacheOperationRequest(
                    BulkOperationType.ADD_ITEM, 0L, 0L, "op-" + i, CACHE_ID, "key-" + i, "val-" + i));
        }
        bulkRequest = new BulkCacheOpsRequest(REQUEST_ID, ops);
    }

    @Override
    public int encodeCreateCache(MutableDirectBuffer buffer) {
        return encoder.encodeCreateCacheRequest(REQUEST_ID, cacheId, buffer);
    }

    @Override
    public int encodeAddCacheEntry(MutableDirectBuffer buffer) {
        return encoder.encodeAddCacheEntry(REQUEST_ID, cacheId, key, value, 0L, buffer);
    }

    @Override
    public int encodeGetCacheEntry(MutableDirectBuffer buffer) {
        return encoder.encodeGetCacheEntry(REQUEST_ID, cacheId, key, buffer);
    }

    @Override
    public int encodeCacheSubscribe(MutableDirectBuffer buffer) {
        return encoder.encodeCacheSubscribe(REQUEST_ID, subscribeCacheIds, subscribeKeys, subscribeModes, true, buffer);
    }

    @Override
    public int encodeBulkOperations(MutableDirectBuffer buffer) {
        return encoder.encodeBulkOperations(REQUEST_ID, bulkRequest, buffer);
    }
}
