package com.bhf.aeroncache.cache;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.services.cachemanager.CacheManagerFactory;
import com.bhf.aeroncache.services.cluster.SBEDecodingCacheClusterService;
import com.bhf.aeroncache.services.tracing.impl.NoOpTracingService;
import com.bhf.aeroncache.types.ReusableString;
import com.bhf.aeroncache.utils.SupplierUtils;
import io.aeron.cluster.service.ClientSession;
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
public class CreateCacheBenchmark {

    /**
     * This benchmark measures pure create throughput and therefore never deletes the caches it
     * creates, so every invocation retains a fresh cache in the manager. Left unbounded, a single
     * measurement iteration accumulates hundreds of thousands of caches and exhausts the heap
     * (OutOfMemoryError in CI). We periodically discard the accumulated state to keep retained
     * caches bounded; the reset is amortized over many thousands of invocations, so its impact on
     * the measured throughput is negligible.
     */
    private static final int RESET_INTERVAL = 50_000;

    SBEDecodingCacheClusterService<ReusableString,ReusableString,ReusableString> sut;
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;
    private ReusableStringCacheRequestEncoder requestEncoder;
    private ReusableStringCacheResponseDecoder responseDecoder;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private CreateCacheResult<ReusableString> createCacheResult;
    private Header header;
    private ClientSession session;
    private long seq;
    private ReusableString reusableCacheId;
    private long tsCounter;
    private String nodeId;
    private NoOpTracingService tracingService;

    @Setup(Level.Trial)
    public void setup() {
        nodeId = "jmh-test-node";
        tracingService = new NoOpTracingService();
        cacheManagerFactory = BenchmarkUtils.getCacheManagerFactory();
        sut = new SBEDecodingCacheClusterService<>(nodeId, tracingService, cacheManagerFactory);

        requestEncoder = new ReusableStringCacheRequestEncoder();
        responseDecoder = new ReusableStringCacheResponseDecoder();
        requestBuffer = new ExpandableArrayBuffer();
        responseBuffer = new ExpandableArrayBuffer();
        header = new Header(0, 0);
        createCacheResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        reusableCacheId = new ReusableString();
        tsCounter = 1;
        
        session = BenchmarkUtils.getMockedSession(responseBuffer);
    }

    @Benchmark
    public void createCache(Blackhole bh) {
        seq++;
        if (seq % RESET_INTERVAL == 0) {
            // Discard the accumulated caches so retained state (and heap usage) stays bounded.
            cacheManagerFactory = BenchmarkUtils.getCacheManagerFactory();
            sut = new SBEDecodingCacheClusterService<>(nodeId, tracingService, cacheManagerFactory);
        }
        reusableCacheId.clear();
        reusableCacheId.copyFrom("cache-" + seq);
        var requestId = "req-" + seq;

        var length = requestEncoder.encodeCreateCacheRequest(requestId, reusableCacheId, requestBuffer);
        
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        responseDecoder.decodeCacheCreated(responseBuffer, 0, createCacheResult);
        
        bh.consume(createCacheResult);
    }

}
