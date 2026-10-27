package com.bhf.aeroncache.codecs.fixtures;

/**
 * Pluggable fixture over one {@link com.bhf.aeroncache.codecs.response.CacheResponseDecoder} implementation.
 * <p>
 * A fixture owns its decoder, a pre-encoded buffer per message (built once in {@link #init(int)}) and the
 * reused result flyweight each message decodes into, and returns that flyweight so the benchmark can consume
 * it. Result types are bound to {@link com.bhf.aeroncache.models.Reusable}, so only the {@code ReusableString}
 * implementation exists today; the {@link Variant} seam lets a further one slot in.
 */
public interface ResponseDecoderFixture {

    /**
     * Build the decoder, encode each response once, and allocate the reused decode targets.
     *
     * @param groupItems entry count for the repeating-group results; keep at or below the encoder's 100-item
     *                   batch size so a single batch is encoded and the decode sees the whole message.
     */
    void init(int groupItems);

    Object decodeCacheCreated();

    Object decodeAddCacheEntryResult();

    Object decodeGetCacheEntryResult();

    Object decodeAllCacheEntriesResult();

    Object decodeBulkCacheOpsResult();

    /** Selectable response-decoder implementations. Each constant builds its fixture. */
    enum Variant {
        REUSABLE_STRING {
            @Override
            public ResponseDecoderFixture create() {
                return new ReusableStringResponseDecoderFixture();
            }
        };

        public abstract ResponseDecoderFixture create();
    }
}
