package com.bhf.aeroncache.codecs.response;

import com.bhf.aeroncache.codecs.fixtures.ResponseEncoderFixture;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Isolated encode-only benchmarks for the server-side response codec. The implementation under test is
 * selected by the {@code variant} {@link Param}, which enumerates every {@link ResponseEncoderFixture.Variant};
 * the fixture owns the codec and its pre-populated result flyweights, so this class is independent of any one
 * encoder implementation. The single-message encoders return the encoded length; the batched group encoders
 * publish via a capturing consumer, so the benchmark consumes the buffer. The paired decode costs live in
 * {@link CacheResponseDecoderBenchmark}.
 */
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
public class CacheResponseEncoderBenchmark {

    /** Response-encoder implementation under test. With no explicit values JMH runs every enum constant. */
    @Param
    public ResponseEncoderFixture.Variant variant;

    /** Length of the entry value encoded by the single-entry results. Matches the cache benchmarks' default. */
    @Param({"160"})
    public int valueLength;

    /** Entry count for the repeating-group results. Kept at or below the encoder's 100-item batch size. */
    @Param({"100"})
    public int groupItems;

    private ResponseEncoderFixture fixture;
    private MutableDirectBuffer buffer;

    @Setup(Level.Trial)
    public void setup() {
        fixture = variant.create();
        fixture.init(groupItems, valueLength);
        buffer = new ExpandableArrayBuffer();
    }

    @Benchmark
    public void encodeCacheCreationResult(Blackhole bh) {
        bh.consume(fixture.encodeCacheCreationResult(buffer));
    }

    @Benchmark
    public void encodeAddCacheEntryResult(Blackhole bh) {
        bh.consume(fixture.encodeAddCacheEntryResult(buffer));
    }

    @Benchmark
    public void encodeCacheEntryResult(Blackhole bh) {
        bh.consume(fixture.encodeCacheEntryResult(buffer));
    }

    @Benchmark
    public void encodeAllCacheEntriesResult(Blackhole bh) {
        fixture.encodeAllCacheEntriesResult(buffer);
        bh.consume(buffer);
    }

    @Benchmark
    public void encodeBulkOpsResponse(Blackhole bh) {
        fixture.encodeBulkOpsResponse(buffer);
        bh.consume(buffer);
    }
}
