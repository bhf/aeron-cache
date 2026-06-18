package com.bhf.aeroncache.cache;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.DeleteCacheResult;
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

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class CreateDeleteCacheBenchmark {

    SBEDecodingCacheClusterService<ReusableString,ReusableString,ReusableString> sut;
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;
    private ReusableStringCacheRequestEncoder requestEncoder;
    private ReusableStringCacheResponseDecoder responseDecoder;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CreateCacheResult<ReusableString> createCacheResult;
    private DeleteCacheResult<ReusableString> deleteResult;
    private Header header;
    private ClientSession session;
    private long seq;
    private ReusableString reusableCacheId;
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
        createCacheResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        deleteResult = new DeleteCacheResult<>(SupplierUtils.stringSupplier.get());
        reusableCacheId = new ReusableString();
        tsCounter = 1;
        
        session = BenchmarkUtils.getMockedSession(responseBuffer);
    }

    @Benchmark
    public void createDeleteCache(Blackhole bh) {
        seq++;
        reusableCacheId.clear();
        reusableCacheId.copyFrom("cache-" + seq);
        var requestId = "req-" + seq;

        var length = requestEncoder.encodeCreateCacheRequest(requestId, reusableCacheId, requestBuffer);
        
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        responseDecoder.decodeCacheCreated(responseBuffer, 0, createCacheResult);
        
        var encodedDeleteCache = requestEncoder.encodeDeleteCache(requestId, reusableCacheId, requestBuffer);
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, encodedDeleteCache, header);
        responseDecoder.decodeCacheDeleted(responseBuffer, 0, deleteResult);

        bh.consume(createCacheResult);
        bh.consume(deleteResult);
    }

}