package com.bhf.aeroncache.codecs.fixtures;

import org.agrona.MutableDirectBuffer;

/**
 * Pluggable fixture over one {@link com.bhf.aeroncache.codecs.request.CacheRequestEncoder} implementation.
 * <p>
 * A fixture owns its codec instance and the type-correct sample inputs it encodes. Encoder implementations
 * need not share a key/value type (the current {@code ReusableString} encoder works on
 * {@link com.bhf.aeroncache.types.ReusableString}; a future impl might encode {@link String} directly), so
 * the benchmark cannot hold them through a single generic field — it goes through this fixture instead. Each
 * method encodes one benchmarked message into the supplied buffer and returns the encoded length.
 * <p>
 * To benchmark a further encoder implementation, add a fixture and a {@link Variant} constant; the benchmark
 * enumerates the variants through its {@code @Param} and needs no other change.
 */
public interface RequestEncoderFixture {

    /**
     * Build the codec and its sample inputs.
     *
     * @param groupItems  item count for the repeating-group messages (subscribe cache ids, bulk operations).
     * @param valueLength length of the entry value used by {@link #encodeAddCacheEntry}.
     */
    void init(int groupItems, int valueLength);

    int encodeCreateCache(MutableDirectBuffer buffer);

    int encodeAddCacheEntry(MutableDirectBuffer buffer);

    int encodeGetCacheEntry(MutableDirectBuffer buffer);

    int encodeCacheSubscribe(MutableDirectBuffer buffer);

    int encodeBulkOperations(MutableDirectBuffer buffer);

    /** Selectable request-encoder implementations. Each constant builds its fixture. */
    enum Variant {
        REUSABLE_STRING {
            @Override
            public RequestEncoderFixture create() {
                return new ReusableStringRequestEncoderFixture();
            }
        };

        public abstract RequestEncoderFixture create();
    }
}
