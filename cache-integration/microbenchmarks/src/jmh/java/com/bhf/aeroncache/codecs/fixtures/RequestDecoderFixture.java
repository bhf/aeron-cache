package com.bhf.aeroncache.codecs.fixtures;

/**
 * Pluggable fixture over one {@link com.bhf.aeroncache.codecs.request.CacheRequestDecoder} implementation.
 * <p>
 * A fixture owns its decoder, a pre-encoded buffer per message (built once in {@link #init(int)}) and the
 * reused request-details flyweight each message decodes into, and returns that flyweight so the benchmark
 * can consume it. Request-detail types are bound to {@link com.bhf.aeroncache.models.Reusable}, so only the
 * {@code ReusableString} implementation exists today; the {@link Variant} seam lets a further one slot in.
 */
public interface RequestDecoderFixture {

    /**
     * Build the decoder, encode each message once, and allocate the reused decode targets.
     *
     * @param groupItems item count for the repeating-group messages (subscribe cache ids, bulk operations).
     */
    void init(int groupItems);

    Object decodeCreateCache();

    Object decodeAddCacheEntry();

    Object decodeGetCacheEntry();

    Object decodeCacheSubscribe();

    Object decodeBulkOperations();

    /** Selectable request-decoder implementations. Each constant builds its fixture. */
    enum Variant {
        REUSABLE_STRING {
            @Override
            public RequestDecoderFixture create() {
                return new ReusableStringRequestDecoderFixture();
            }
        };

        public abstract RequestDecoderFixture create();
    }
}
