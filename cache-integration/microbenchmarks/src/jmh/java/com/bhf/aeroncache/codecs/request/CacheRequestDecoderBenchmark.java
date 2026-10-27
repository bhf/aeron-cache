package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.codecs.fixtures.RequestDecoderFixture;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Isolated decode-only benchmarks for the server-side request codec. The implementation under test is
 * selected by the {@code variant} {@link Param}, which enumerates every {@link RequestDecoderFixture.Variant};
 * the fixture encodes each message once up front and holds the reused decode target, so this class is
 * independent of any one decoder implementation. Each measured invocation decodes a pre-encoded buffer,
 * isolating the SBE decode and field-copy work from the cluster-service round-trip. The paired encode costs
 * live in {@link CacheRequestEncoderBenchmark}.
 */
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class CacheRequestDecoderBenchmark {

    /** Request-decoder implementation under test. With no explicit values JMH runs every enum constant. */
    @Param
    public RequestDecoderFixture.Variant variant;

    /** Item count for the repeating-group messages (subscribe cache ids, bulk operations). */
    @Param({"100"})
    public int groupItems;

    private RequestDecoderFixture fixture;

    @Setup(Level.Trial)
    public void setup() {
        fixture = variant.create();
        fixture.init(groupItems);
    }

    @Benchmark
    public void decodeCreateCache(Blackhole bh) {
        bh.consume(fixture.decodeCreateCache());
    }

    @Benchmark
    public void decodeAddCacheEntry(Blackhole bh) {
        bh.consume(fixture.decodeAddCacheEntry());
    }

    @Benchmark
    public void decodeGetCacheEntry(Blackhole bh) {
        bh.consume(fixture.decodeGetCacheEntry());
    }

    @Benchmark
    public void decodeCacheSubscribe(Blackhole bh) {
        bh.consume(fixture.decodeCacheSubscribe());
    }

    @Benchmark
    public void decodeBulkOperations(Blackhole bh) {
        bh.consume(fixture.decodeBulkOperations());
    }
}
