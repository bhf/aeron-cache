package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.services.cachemanager.impl.AbstractHashMapCacheManager;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

import java.util.function.Consumer;

/**
 * A factory to create cache manager instances.
 *
 * @param <I> The type on which caches are indexed.
 * @param <K> The type of the key for cache entries which this factory will create.
 * @param <V> The type of the value for cache entries which this factory will create.
 */
public class CacheManagerFactory<I, K, V> {
    public CacheManager<I, K, V> getCacheManager(Consumer<ExclusivePublication> takeSnapshotProcessor, Consumer<Image> loadSnapshotProcessor) {
        return new AbstractHashMapCacheManager<>() {

            @Override
            public void takeSnapshot(ExclusivePublication snapshotPublication) {
                takeSnapshotProcessor.accept(snapshotPublication);
            }

            @Override
            public void loadSnapshot(Image snapshotImage) {
                loadSnapshotProcessor.accept(snapshotImage);
            }

        };
    }
}
