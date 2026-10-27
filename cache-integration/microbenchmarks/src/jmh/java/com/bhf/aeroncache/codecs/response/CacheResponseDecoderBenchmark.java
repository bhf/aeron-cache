package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.codecs.fixtures.ResponseDecoderFixture;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Isolated decode-only benchmarks for the client-side response codec. The implementation under test is
 * selected by the {@code variant} {@link Param}, which enumerates every {@link ResponseDecoderFixture.Variant};
 * the fixture encodes each response once up front and holds the reused decode target, so this class is
 * independent of any one decoder implementation. Each measured invocation decodes a pre-encoded buffer,
 * isolating the SBE decode and field-copy work (including the per-entry allocation the group decoders perform)
 * from any transport. The paired encode costs live in {@link CacheResponseEncoderBenchmark}.
 */
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class CacheResponseDecoderBenchmark {

    /** Response-decoder implementation under test. With no explicit values JMH runs every enum constant. */
    @Param
    public ResponseDecoderFixture.Variant variant;

    /** Entry count for the repeating-group results. Kept at or below the encoder's 100-item batch size. */
    @Param({"100"})
    public int groupItems;

    private ResponseDecoderFixture fixture;

    @Setup(Level.Trial)
    public void setup() {
        fixture = variant.create();
        fixture.init(groupItems);
    }

    @Benchmark
    public void decodeCacheCreated(Blackhole bh) {
        bh.consume(fixture.decodeCacheCreated());
    }

    @Benchmark
    public void decodeAddCacheEntryResult(Blackhole bh) {
        bh.consume(fixture.decodeAddCacheEntryResult());
    }

    @Benchmark
    public void decodeGetCacheEntryResult(Blackhole bh) {
        bh.consume(fixture.decodeGetCacheEntryResult());
    }

    @Benchmark
    public void decodeAllCacheEntriesResult(Blackhole bh) {
        bh.consume(fixture.decodeAllCacheEntriesResult());
    }

    @Benchmark
    public void decodeBulkCacheOpsResult(Blackhole bh) {
        bh.consume(fixture.decodeBulkCacheOpsResult());
    }
}
