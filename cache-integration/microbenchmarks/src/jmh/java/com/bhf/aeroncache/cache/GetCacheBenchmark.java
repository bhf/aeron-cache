package com.bhf.aeroncache.cache;

import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.GetCacheEntryResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.logbuffer.Header;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class GetCacheBenchmark {

    @Param({"100"})
    public int cacheSize;

    private SBEDecodingCacheClusterService<ReusableString,ReusableString,ReusableString> sut;
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;
    private ReusableStringCacheRequestEncoder requestEncoder;
    private ReusableStringCacheResponseDecoder responseDecoder;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private GetCacheEntryResult<ReusableString, ReusableString, ReusableString> result;
    private Header header;
    private ClientSession session;
    private long seq;
    
    private ReusableString reusableCacheId;
    private ReusableString reusableKey;
    private ReusableString reusableValue;
    private long tsCounter;

    @Setup(Level.Trial)
    public void setup() {
        var nodeId = "jmh-test-node";
        var tracingService = new NoOpTracingService();
        cacheManagerFactory = BenchmarkUtils.getCacheManagerFactory();
        sut = new SBEDecodingCacheClusterService<>(nodeId, tracingService, cacheManagerFactory);
        Cluster clusterStub = BenchmarkUtils.getClusterStub();
        sut.onStart(clusterStub, null);

        requestEncoder = new ReusableStringCacheRequestEncoder();
        responseDecoder = new ReusableStringCacheResponseDecoder();
        requestBuffer = new ExpandableArrayBuffer();
        responseBuffer = new ExpandableArrayBuffer();
        header = new Header(0, 0);

        result = new GetCacheEntryResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());
        
        reusableCacheId = new ReusableString();
        reusableKey = new ReusableString();
        reusableValue = new ReusableString();
        tsCounter = 1;

        session = BenchmarkUtils.getMockedSession(responseBuffer);

        reusableCacheId.copyFrom("jmh-cache");
        String createRequestId = UUID.randomUUID().toString();
        
        int length = requestEncoder.encodeCreateCacheRequest(createRequestId, reusableCacheId, requestBuffer);
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        
        CreateCacheResult<ReusableString> createResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        responseDecoder.decodeCacheCreated(responseBuffer, 0, createResult);

        for (int i = 0; i < cacheSize; i++) {
            reusableKey.clear();
            reusableKey.copyFrom("key-" + i);
            
            reusableValue.clear();
            reusableValue.copyFrom("val-" + i);
            
            String requestId = UUID.randomUUID().toString();
            int addLength = requestEncoder.encodeAddCacheEntry(
                    requestId, reusableCacheId, reusableKey, reusableValue, 0L, requestBuffer);
            
            sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, addLength, header);
        }
    }

    @Benchmark
    public void getCacheEntry(Blackhole bh) {
        seq++;
        long index = seq % cacheSize;
        reusableKey.clear();
        reusableKey.copyFrom("key-" + index);
        
        String requestId = "req-" + seq;

        int length = requestEncoder.encodeGetCacheEntry(
                requestId, reusableCacheId, reusableKey, requestBuffer);
        
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        responseDecoder.decodeGetCacheEntryResult(responseBuffer, 0, result);
        
        bh.consume(result);
    }

}
