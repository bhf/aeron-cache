package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.services.cachemanager.impl.AbstractHashMapCacheManager;
import io.aeron.ExclusivePublication;
import io.aeron.Image;

import java.util.function.Consumer;

public class CacheManagerFactory<I,K,V> {
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
