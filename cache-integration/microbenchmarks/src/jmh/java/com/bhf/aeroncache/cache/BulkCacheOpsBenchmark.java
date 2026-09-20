package com.bhf.aeroncache.cache;

import com.bhf.aeroncache.cache.utils.BenchmarkUtils;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseDecoder;
import com.bhf.aeroncache.models.bulk.requests.BulkCacheOpsRequest;
import com.bhf.aeroncache.models.bulk.requests.BulkOperationType;
import com.bhf.aeroncache.models.bulk.requests.CacheOperationRequest;
import com.bhf.aeroncache.models.results.BulkCacheOpsResult;
import com.bhf.aeroncache.models.results.CreateCacheResult;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class BulkCacheOpsBenchmark {

    private static final String CACHE_ID = "jmh-bulk-cache";

    /**
     * The number of add/get/remove triples in each bulk request. Keys are reused across invocations
     * so the batch is self-balancing (every add is matched by a remove) and retained state stays
     * bounded.
     */
    @Param({"10", "100", "1000"})
    public int batchSize;

    private SBEDecodingCacheClusterService<ReusableString,ReusableString,ReusableString> sut;
    private CacheManagerFactory<ReusableString, ReusableString, ReusableString> cacheManagerFactory;
    private ReusableStringCacheRequestEncoder requestEncoder;
    private ReusableStringCacheResponseDecoder responseDecoder;
    private MutableDirectBuffer requestBuffer;
    private MutableDirectBuffer responseBuffer;
    private BulkCacheOpsResult<ReusableString, ReusableString, ReusableString> bulkResult;
    private Header header;
    private ClientSession session;
    private long seq;
    private long tsCounter;

    private List<CacheOperationRequest> ops;

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

        bulkResult = new BulkCacheOpsResult<>(SupplierUtils.stringSupplier, SupplierUtils.stringSupplier, SupplierUtils.stringSupplier);

        tsCounter = 1;

        session = BenchmarkUtils.getMockedSession(responseBuffer);

        var reusableCacheId = new ReusableString();
        reusableCacheId.copyFrom(CACHE_ID);
        var createRequestId = UUID.randomUUID().toString();
        var length = requestEncoder.encodeCreateCacheRequest(createRequestId, reusableCacheId, requestBuffer);
        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);

        var createResult = new CreateCacheResult<>(SupplierUtils.stringSupplier.get());
        responseDecoder.decodeCacheCreated(responseBuffer, 0, createResult);

        ops = new ArrayList<>(batchSize * 3);
        for (int i = 0; i < batchSize; i++) {
            var key = "key-" + i;
            ops.add(new CacheOperationRequest(BulkOperationType.ADD_ITEM, 0L, 0L, "add-" + i, CACHE_ID, key, "val-" + i));
            ops.add(new CacheOperationRequest(BulkOperationType.GET_ITEM, 0L, 0L, "get-" + i, CACHE_ID, key, ""));
            ops.add(new CacheOperationRequest(BulkOperationType.REMOVE_ITEM, 0L, 0L, "remove-" + i, CACHE_ID, key, ""));
        }
    }

    @Benchmark
    public void bulkCacheOps(Blackhole bh) {
        seq++;
        var requestId = "req-" + seq;

        var bulkRequest = new BulkCacheOpsRequest(requestId, ops);
        var length = requestEncoder.encodeBulkOperations(requestId, bulkRequest, requestBuffer);

        sut.onSessionMessage(session, ++tsCounter, requestBuffer, 0, length, header);
        responseDecoder.decodeBulkCacheOpsResult(responseBuffer, 0, bulkResult);

        bh.consume(bulkResult);
    }

}
