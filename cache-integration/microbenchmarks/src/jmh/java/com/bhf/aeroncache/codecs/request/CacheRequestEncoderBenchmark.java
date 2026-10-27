package com.bhf.aeroncache.codecs.request;

import com.bhf.aeroncache.codecs.fixtures.RequestEncoderFixture;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Isolated encode-only benchmarks for the client-side request codec. The implementation under test is
 * selected by the {@code variant} {@link Param}, which enumerates every
 * {@link RequestEncoderFixture.Variant}; the fixture owns the codec and its type-correct sample inputs, so
 * this class is independent of any one encoder implementation. Each method measures a single {@code encode*}
 * call into a reused buffer, isolating the codec work (SBE encode plus whatever string conversions the
 * implementation performs) from any cluster-service round-trip. The paired decode costs live in
 * {@link CacheRequestDecoderBenchmark}.
 */
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class CacheRequestEncoderBenchmark {

    /** Request-encoder implementation under test. With no explicit values JMH runs every enum constant. */
    @Param
    public RequestEncoderFixture.Variant variant;

    /** Length of the entry value encoded by {@link #encodeAddCacheEntry}. Matches the cache benchmarks' default. */
    @Param({"160"})
    public int valueLength;

    /** Item count for the repeating-group messages (subscribe cache ids, bulk operations). */
    @Param({"100"})
    public int groupItems;

    private RequestEncoderFixture fixture;
    private MutableDirectBuffer buffer;

    @Setup(Level.Trial)
    public void setup() {
        fixture = variant.create();
        fixture.init(groupItems, valueLength);
        buffer = new ExpandableArrayBuffer();
    }

    @Benchmark
    public void encodeCreateCache(Blackhole bh) {
        bh.consume(fixture.encodeCreateCache(buffer));
    }

    @Benchmark
    public void encodeAddCacheEntry(Blackhole bh) {
        bh.consume(fixture.encodeAddCacheEntry(buffer));
    }

    @Benchmark
    public void encodeGetCacheEntry(Blackhole bh) {
        bh.consume(fixture.encodeGetCacheEntry(buffer));
    }

    @Benchmark
    public void encodeCacheSubscribe(Blackhole bh) {
        bh.consume(fixture.encodeCacheSubscribe(buffer));
    }

    @Benchmark
    public void encodeBulkOperations(Blackhole bh) {
        bh.consume(fixture.encodeBulkOperations(buffer));
    }
}
