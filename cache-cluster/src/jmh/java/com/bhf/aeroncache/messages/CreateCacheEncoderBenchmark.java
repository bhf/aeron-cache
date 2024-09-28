package com.bhf.aeroncache.messages;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH Benchmark for the Create Cache Encoder. Compares different ways
 * to use the blackhole, as well as a control case where we do nothing.
 */
@State(Scope.Benchmark)
public class CreateCacheEncoderBenchmark {

    private CreateCacheEncoder enc;
    private MutableDirectBuffer buffer;
    private MessageHeaderEncoder headerEncoder;
    private long cacheId = 0;

    /**
     * Setup the benchmark.
     */
    @Setup(Level.Trial)
    public void setup() {
        enc = new CreateCacheEncoder();
        buffer = new ExpandableArrayBuffer();
        headerEncoder = new MessageHeaderEncoder();
    }

    /**
     * Encode a Create Cache event and blackhole the encoder used.
     *
     * @param bh The blackhole.
     */
    @Benchmark
    public void encodeBHEncoder(Blackhole bh) {
        enc.wrapAndApplyHeader(buffer, 0, headerEncoder);
        enc.cacheId(++cacheId);
        bh.consume(enc);
    }

    /**
     * Encode a Create Cache event and blackhole the cache Id.
     *
     * @param bh The blackhole.
     */
    @Benchmark
    public void encodeBHCacheId(Blackhole bh) {
        enc.wrapAndApplyHeader(buffer, 0, headerEncoder);
        enc.cacheId(++cacheId);
        bh.consume(cacheId);
    }

    /**
     * Encode a Create Cache event and blackhole nothing.
     *
     * @param bh The blackhole.
     */
    @Benchmark
    public void encodeNoBH(Blackhole bh) {
        enc.wrapAndApplyHeader(buffer, 0, headerEncoder);
        enc.cacheId(++cacheId);
    }

    /**
     * Don't encode a Create Cache event.
     * Simply increment a long and blackhole it.
     *
     * @param bh The blackhole.
     */
    @Benchmark
    public void doNothingBHCacheId(Blackhole bh) {
        bh.consume(++cacheId);
    }

}
