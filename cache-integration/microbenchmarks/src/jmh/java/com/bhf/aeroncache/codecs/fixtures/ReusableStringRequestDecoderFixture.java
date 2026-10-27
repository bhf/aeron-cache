package com.bhf.aeroncache.codecs.fixtures;

import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.requests.AddCacheEntryRequestDetails;
import com.bhf.aeroncache.models.requests.BulkCacheOpsRequestDetails;
import com.bhf.aeroncache.models.requests.CacheSubscriptionRequestDetails;
import com.bhf.aeroncache.models.requests.CreateCacheRequestDetails;
import com.bhf.aeroncache.models.requests.GetCacheEntryRequestDetails;
import com.bhf.aeroncache.models.requests.SubscriptionMode;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import java.util.ArrayList;

/**
 * {@link RequestDecoderFixture} backed by {@link ReusableStringCacheRequestDecoder}. Buffers are encoded
 * once with the matching {@link ReusableStringCacheRequestEncoder}; each decode reuses a single
 * request-details flyweight. The two pooled targets (subscribe, bulk) are recycled at the start of every
 * decode so the backing object pools stay balanced and the measured allocation reflects steady-state server
 * reuse rather than unbounded pool growth.
 */
public class ReusableStringRequestDecoderFixture implements RequestDecoderFixture {

    private static final String REQUEST_ID = "jmh-request-id";
    private static final String CACHE_ID = "jmh-codec-cache";

    private CacheRequestDecoder<ReusableString, ReusableString, ReusableString> decoder;

    private MutableDirectBuffer createBuffer;
    private MutableDirectBuffer addBuffer;
    private MutableDirectBuffer getBuffer;
    private MutableDirectBuffer subscribeBuffer;
    private MutableDirectBuffer bulkBuffer;

    private CreateCacheRequestDetails<ReusableString> createDetails;
    private AddCacheEntryRequestDetails<ReusableString, ReusableString, ReusableString> addDetails;
    private GetCacheEntryRequestDetails<ReusableString, ReusableString> getDetails;
    private CacheSubscriptionRequestDetails<ReusableString, ReusableString> subscribeDetails;
    private BulkCacheOpsRequestDetails<ReusableString, ReusableString, ReusableString> bulkDetails;

    @Override
    public void init(int groupItems) {
        decoder = new ReusableStringCacheRequestDecoder();

        var encoder = new ReusableStringCacheRequestEncoder();

        var cacheId = new ReusableString();
        cacheId.copyFrom(CACHE_ID);
        var key = new ReusableString();
        key.copyFrom("jmh-key");
        var value = new ReusableString();
        value.copyFrom("v".repeat(160));

        createBuffer = new ExpandableArrayBuffer();
        encoder.encodeCreateCacheRequest(REQUEST_ID, cacheId, createBuffer);

        addBuffer = new ExpandableArrayBuffer();
        encoder.encodeAddCacheEntry(REQUEST_ID, cacheId, key, value, 0L, addBuffer);

        getBuffer = new ExpandableArrayBuffer();
        encoder.encodeGetCacheEntry(REQUEST_ID, cacheId, key, getBuffer);

        var subscribeCacheIds = new ArrayList<ReusableString>(groupItems);
        var subscribeKeys = new ArrayList<ReusableString>(groupItems);
        var subscribeModes = new ArrayList<SubscriptionMode>(groupItems);
        for (int i = 0; i < groupItems; i++) {
            var id = new ReusableString();
            id.copyFrom("cache-" + i);
            subscribeCacheIds.add(id);
            subscribeKeys.add(null);
            subscribeModes.add(SubscriptionMode.FULL);
        }
        subscribeBuffer = new ExpandableArrayBuffer();
        encoder.encodeCacheSubscribe(REQUEST_ID, subscribeCacheIds, subscribeKeys, subscribeModes, true, subscribeBuffer);

        var ops = new ArrayList<CacheOperationRequest>(groupItems);
        for (int i = 0; i < groupItems; i++) {
            ops.add(new CacheOperationRequest(
                    BulkOperationType.ADD_ITEM, 0L, 0L, "op-" + i, CACHE_ID, "key-" + i, "val-" + i));
        }
        bulkBuffer = new ExpandableArrayBuffer();
        encoder.encodeBulkOperations(REQUEST_ID, new BulkCacheOpsRequest(REQUEST_ID, ops), bulkBuffer);

        createDetails = new CreateCacheRequestDetails<>(SupplierUtils.stringSupplier.get());
        addDetails = new AddCacheEntryRequestDetails<>(
                SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        getDetails = new GetCacheEntryRequestDetails<>(
                SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        subscribeDetails = new CacheSubscriptionRequestDetails<>(
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        bulkDetails = new BulkCacheOpsRequestDetails<>(
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
    }

    @Override
    public Object decodeCreateCache() {
        decoder.decodeGetCreateCacheRequestDetails(createBuffer, 0, createDetails);
        return createDetails;
    }

    @Override
    public Object decodeAddCacheEntry() {
        decoder.decodeAddCacheEntryRequest(addBuffer, 0, addDetails);
        return addDetails;
    }

    @Override
    public Object decodeGetCacheEntry() {
        decoder.decodeGetCacheEntryRequest(getBuffer, 0, getDetails);
        return getDetails;
    }

    @Override
    public Object decodeCacheSubscribe() {
        // Return the previous invocation's pooled cache ids/keys before re-decoding, mirroring the server's
        // reuse so the backing pool stays balanced and allocation does not grow each invocation.
        subscribeDetails.recycle();
        decoder.decodeCacheSubscriptionRequest(subscribeBuffer, 0, subscribeDetails);
        return subscribeDetails;
    }

    @Override
    public Object decodeBulkOperations() {
        // As above: return the previous invocation's pooled operation flyweights before re-decoding.
        bulkDetails.recycle();
        decoder.decodeBulkCacheOperationsRequest(bulkBuffer, 0, bulkDetails);
        return bulkDetails;
    }
}
