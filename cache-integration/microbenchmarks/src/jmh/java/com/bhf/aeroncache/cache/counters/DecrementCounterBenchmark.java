package com.bhf.aeroncache.cache.counters;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.request.ReusableStringCountersCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCountersCacheResponseDecoder;
import com.bhf.aeroncache.models.ReusableLong;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.models.results.DecrementCounterResult;
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
public class DecrementCounterBenchmark {

    private static final String COUNTER_KEY = "counter";

    private SBEDecodingCacheClusterService<ReusableString,ReusableString,ReusableString> sut;
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;
    private ReusableStringCountersCacheRequestEncoder requestEncoder;
    private ReusableStringCountersCacheResponseDecoder responseDecoder;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private DecrementCounterResult<ReusableString, ReusableString> decrementCounterResult;
    private Header header;
    private ClientSession session;
    private long seq;

    private ReusableString reusableCacheId;
    private ReusableString reusableKey;
    private long tsCounter;

    @Setup(Level.Trial)
    public void setup() {
        var nodeId = "jmh-test-node";
        var tracingService = new NoOpTracingService();
        cacheManagerFactory = BenchmarkUtils.getCacheManagerFactory();
        sut = new SBEDecodingCacheClusterService<>(nodeId, tracingService, cacheManagerFactory);
        Cluster clusterStub = BenchmarkUtils.getClusterStub();
        sut.onStart(clusterStub, null);

        requestEncoder = new ReusableStringCountersCacheRequestEncoder();
        responseDecoder = new ReusableStringCountersCacheResponseDecoder();
        requestBuffer = new ExpandableArrayBuffer();
        responseBuffer = new ExpandableArrayBuffer();
        header = new Header(0, 0);

        decrementCounterResult = new DecrementCounterResult<>(SupplierUtils.stringSupplier.get(), SupplierUtils.stringSupplier.get());

        reusableCacheId = new ReusableString();
        reusableKey = new ReusableString();
        tsCounter = 1;

        session = BenchmarkUtils.getMockedSession(responseBuffer);

        reusableCacheId.copyFrom("jmh-counter-cache");
        var createRequestId = UUID.randomUUID().toString();
        var length = requestEncoder.encodeCreateCacheRequest(createRequestId, reusableCacheId, requestBuffer);
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);

        var createResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        responseDecoder.decodeCacheCreated(responseBuffer, 0, createResult);

        reusableKey.copyFrom(COUNTER_KEY);
        var initialValue = new ReusableLong();
        initialValue.copyFrom(0L);
        var addRequestId = UUID.randomUUID().toString();
        int addLength = requestEncoder.encodeAddCacheEntry(addRequestId, reusableCacheId, reusableKey, initialValue, 0L, requestBuffer);
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, addLength, header);
    }

    @Benchmark
    public void decrementCounter(Blackhole bh) {
        seq++;
        var requestId = "req-" + seq;

        var length = requestEncoder.encodeDecrementCounterRequest(
                requestId, reusableCacheId, reusableKey, 1L, 0L, requestBuffer);

        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        responseDecoder.decodeDecrementCounterResponse(responseBuffer, 0, decrementCounterResult);

        bh.consume(decrementCounterResult);
    }

}
