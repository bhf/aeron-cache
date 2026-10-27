package com.bhf.aeroncache.codecs.fixtures;

import org.agrona.MutableDirectBuffer;

/**
 * Pluggable fixture over one {@link com.bhf.aeroncache.codecs.response.CacheResponseEncoder} implementation.
 * <p>
 * A fixture owns its codec and the pre-populated result flyweights it encodes. The single-message encoders
 * return the encoded length; the batched group encoders publish each batch through a
 * {@link com.bhf.aeroncache.models.consumer.HydratingPublicationConsumer} the fixture holds, so they return
 * {@code void} and the benchmark consumes the buffer. Result types are bound to
 * {@link com.bhf.aeroncache.models.Reusable}, so only the {@code ReusableString} implementation exists today;
 * the {@link Variant} seam lets a further one slot in.
 */
public interface ResponseEncoderFixture {

    /**
     * Build the codec and the result flyweights.
     *
     * @param groupItems  entry count for the repeating-group results; keep at or below the encoder's 100-item
     *                    batch size so a single batch is produced.
     * @param valueLength length of the entry value used by the single-entry results.
     */
    void init(int groupItems, int valueLength);

    int encodeCacheCreationResult(MutableDirectBuffer buffer);

    int encodeAddCacheEntryResult(MutableDirectBuffer buffer);

    int encodeCacheEntryResult(MutableDirectBuffer buffer);

    void encodeAllCacheEntriesResult(MutableDirectBuffer buffer);

    void encodeBulkOpsResponse(MutableDirectBuffer buffer);

    /** Selectable response-encoder implementations. Each constant builds its fixture. */
    enum Variant {
        REUSABLE_STRING {
            @Override
            public ResponseEncoderFixture create() {
                return new ReusableStringResponseEncoderFixture();
            }
        };

        public abstract ResponseEncoderFixture create();
    }
}
