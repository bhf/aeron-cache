package com.bhf.aeroncache.codecs.fixtures;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
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
import org.agrona.MutableDirectBuffer;

/**
 * {@link ResponseEncoderFixture} backed by {@link ReusableStringCacheResponseEncoder}, encoding from
 * {@link ReusableString}-typed result flyweights. The batched group encoders publish via the capturing
 * {@link HydratingPublicationConsumer} from {@link BenchmarkUtils}.
 */
public class ReusableStringResponseEncoderFixture implements ResponseEncoderFixture {

    private static final String REQUEST_ID = "jmh-request-id";
    private static final String CACHE_ID = "jmh-codec-cache";

    private CacheResponseEncoder<ReusableString, ReusableString, ReusableString> encoder;
    private HydratingPublicationConsumer consumer;

    private ReusableString cacheId;
    private ReusableString key;

    private CreateCacheResult<ReusableString> createResult;
    private AddCacheEntryResult<ReusableString, ReusableString> addResult;
    private GetCacheEntryResult<ReusableString, ReusableString, ReusableString> getResult;
    private GetAllCacheEntriesResult<ReusableString, ReusableString, ReusableString> getAllResult;
    private BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkResult;

    @Override
    public void init(int groupItems, int valueLength) {
        encoder = new ReusableStringCacheResponseEncoder();
        consumer = BenchmarkUtils.getCapturingConsumer();

        cacheId = new ReusableString();
        cacheId.copyFrom(CACHE_ID);
        key = new ReusableString();
        key.copyFrom("jmh-key");
        var value = new ReusableString();
        value.copyFrom("v".repeat(valueLength));

        createResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        createResult.getCacheId().copyFrom(cacheId);
        createResult.setRequestId(REQUEST_ID);
        createResult.setStatus(CacheOperationStatus.SUCCESS);

        addResult = new AddCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        addResult.getCacheId().copyFrom(cacheId);
        addResult.getEntryKey().copyFrom(key);
        addResult.setRequestId(REQUEST_ID);
        addResult.setStatus(CacheOperationStatus.SUCCESS);

        getResult = new GetCacheEntryResult<>(
                SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        getResult.getCacheId().copyFrom(cacheId);
        getResult.getEntryKey().copyFrom(key);
        getResult.getEntryValue().copyFrom(value);
        getResult.setRequestId(REQUEST_ID);
        getResult.setStatus(CacheOperationStatus.SUCCESS);

        getAllResult = new GetAllCacheEntriesResult<>(SupplierUtils.stringSupplier.get());
        getAllResult.getCacheId().copyFrom(cacheId);
        getAllResult.setRequestId(REQUEST_ID);
        getAllResult.setStatus(CacheOperationStatus.SUCCESS);
        for (int i = 0; i < groupItems; i++) {
            var k = new ReusableString();
            k.copyFrom("key-" + i);
            var v = new ReusableString();
            v.copyFrom("val-" + i);
            getAllResult.getValues().put(k, v);
        }

        bulkResult = new BulkCacheOpsResult<>(
                SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);
        bulkResult.setRequestId(REQUEST_ID);
        var opKey = new ReusableString();
        var opValue = new ReusableString();
        for (int i = 0; i < groupItems; i++) {
            opKey.clear();
            opKey.copyFrom("key-" + i);
            opValue.clear();
            opValue.copyFrom("val-" + i);
            bulkResult.addOperationResult(CacheOperationStatus.SUCCESS, "op-" + i, cacheId, opKey, opValue);
        }
    }

    @Override
    public int encodeCacheCreationResult(MutableDirectBuffer buffer) {
        return encoder.encodeCacheCreationResult(cacheId, createResult, buffer);
    }

    @Override
    public int encodeAddCacheEntryResult(MutableDirectBuffer buffer) {
        return encoder.encodeAddCacheEntryResult(cacheId, key, addResult, buffer);
    }

    @Override
    public int encodeCacheEntryResult(MutableDirectBuffer buffer) {
        return encoder.encodeCacheEntryResult(cacheId, getResult, buffer);
    }

    @Override
    public void encodeAllCacheEntriesResult(MutableDirectBuffer buffer) {
        encoder.encodeAllCacheEntriesResult(cacheId, getAllResult, buffer, consumer);
    }

    @Override
    public void encodeBulkOpsResponse(MutableDirectBuffer buffer) {
        encoder.encodeBulkOpsResponse(bulkResult, buffer, consumer);
    }
}
