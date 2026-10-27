package com.bhf.aeroncache.codecs.fixtures;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.response.CacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer;
import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.GetAllCacheEntriesResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * {@link ResponseDecoderFixture} backed by {@link ReusableStringCacheResponseDecoder}. Buffers are encoded
 * once with the matching {@link ReusableStringCacheResponseEncoder}; each decode reuses a single result
 * flyweight. No manual recycling is needed: {@link BulkCacheOpsResult#clear()} returns its pooled operation
 * results internally, and the per-entry {@link ReusableString} allocation the group decoders perform is
 * inherent to the decoder and therefore part of what is measured.
 */
public class ReusableStringResponseDecoderFixture implements ResponseDecoderFixture {

    private static final String REQUEST_ID = "jmh-request-id";
    private static final String CACHE_ID = "jmh-codec-cache";

    private CacheResponseDecoder<ReusableString, ReusableString, ReusableString> decoder;

    private MutableDirectBuffer createBuffer;
    private MutableDirectBuffer addBuffer;
    private MutableDirectBuffer getBuffer;
    private MutableDirectBuffer getAllBuffer;
    private MutableDirectBuffer bulkBuffer;

    private CreateCacheResult<ReusableString> createResult;
    private AddCacheEntryResult<ReusableString, ReusableString> addResult;
    private GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getResult;
    private GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllResult;
    private BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkResult;

    @Override
    public void init(int groupItems) {
        decoder = new ReusableStringCacheResponseDecoder();

        var encoder = new ReusableStringCacheResponseEncoder();
        HydratingPublicationConsumer consumer = BenchmarkUtils.getCapturingConsumer();

        var cacheId = new ReusableString();
        cacheId.copyFrom(CACHE_ID);
        var key = new ReusableString();
        key.copyFrom("jmh-key");
        var value = new ReusableString();
        value.copyFrom("v".repeat(160));

        var createSource = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        createSource.getCacheId().copyFrom(cacheId);
        createSource.setRequestId(REQUEST_ID);
        createSource.setStatus(CacheOperationStatus.SUCCESS);
        createBuffer = new ExpandableArrayBuffer();
        encoder.encodeCacheCreationResult(cacheId, createSource, createBuffer);

        var addSource = new AddCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        addSource.getCacheId().copyFrom(cacheId);
        addSource.getEntryKey().copyFrom(key);
        addSource.setRequestId(REQUEST_ID);
        addSource.setStatus(CacheOperationStatus.SUCCESS);
        addBuffer = new ExpandableArrayBuffer();
        encoder.encodeAddCacheEntryResult(cacheId, key, addSource, addBuffer);

        var getSource = new GetCacheEntryResult<>(
                SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        getSource.getCacheId().copyFrom(cacheId);
        getSource.getEntryKey().copyFrom(key);
        getSource.getEntryValue().copyFrom(value);
        getSource.setRequestId(REQUEST_ID);
        getSource.setStatus(CacheOperationStatus.SUCCESS);
        getBuffer = new ExpandableArrayBuffer();
        encoder.encodeCacheEntryResult(cacheId, getSource, getBuffer);

        var getAllSource = new GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString>(SupplierUtils.stringSupplier.get());
        getAllSource.getCacheId().copyFrom(cacheId);
        getAllSource.setRequestId(REQUEST_ID);
        getAllSource.setStatus(CacheOperationStatus.SUCCESS);
        for (int i = 0; i < groupItems; i++) {
            var k = new ReusableString();
            k.copyFrom("key-" + i);
            var v = new ReusableString();
            v.copyFrom("val-" + i);
            getAllSource.getValues().put(k, v);
        }
        getAllBuffer = new ExpandableArrayBuffer();
        encoder.encodeAllCacheEntriesResult(cacheId, getAllSource, getAllBuffer, consumer);

        var bulkSource = new BulkCacheOpsResult<>(
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        bulkSource.setRequestId(REQUEST_ID);
        var opKey = new ReusableString();
        var opValue = new ReusableString();
        for (int i = 0; i < groupItems; i++) {
            opKey.clear();
            opKey.copyFrom("key-" + i);
            opValue.clear();
            opValue.copyFrom("val-" + i);
            bulkSource.addOperationResult(CacheOperationStatus.SUCCESS, "op-" + i, cacheId, opKey, opValue);
        }
        bulkBuffer = new ExpandableArrayBuffer();
        encoder.encodeBulkOpsResponse(bulkSource, bulkBuffer, consumer);

        createResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        addResult = new AddCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        getResult = new GetCacheEntryResult<>(
                SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        getAllResult = new GetAllCacheEntriesResult<>(SupplierUtils.stringSupplier.get());
        bulkResult = new BulkCacheOpsResult<>(
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
    }

    @Override
    public Object decodeCacheCreated() {
        decoder.decodeCacheCreated(createBuffer, 0, createResult);
        return createResult;
    }

    @Override
    public Object decodeAddCacheEntryResult() {
        decoder.decodeAddCacheEntryResult(addBuffer, 0, addResult);
        return addResult;
    }

    @Override
    public Object decodeGetCacheEntryResult() {
        decoder.decodeGetCacheEntryResult(getBuffer, 0, getResult);
        return getResult;
    }

    @Override
    public Object decodeAllCacheEntriesResult() {
        decoder.decodeAllCacheEntriesResult(getAllBuffer, 0, getAllResult);
        return getAllResult;
    }

    @Override
    public Object decodeBulkCacheOpsResult() {
        decoder.decodeBulkCacheOpsResult(bulkBuffer, 0, bulkResult);
        return bulkResult;
    }
}
