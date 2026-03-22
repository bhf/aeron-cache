package com.bhf.aeroncache.services.cachemanager;

import com.bhf.aeroncache.codecs.request.CacheRequestDecoder;
import com.bhf.aeroncache.codecs.request.ReusableStringCacheRequestDecoder;
import com.bhf.aeroncache.codecs.response.CacheResponseEncoder;
import com.bhf.aeroncache.codecs.response.ReusableStringCacheResponseEncoder;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.utils.SupplierUtils;

import java.util.function.Supplier;

public class DefaultCacheManagerFactory extends BasicCacheManagerFactory<Reusable<?>, Reusable<?>, Reusable<?>> {

    public DefaultCacheManagerFactory() {
        super((Supplier) SupplierUtils.stringSupplier,
                (Supplier) SupplierUtils.stringSupplier,
                (Supplier) SupplierUtils.stringSupplier,
                (Supplier) SupplierUtils.mapSupplier,
                (CacheIdSnapshotCodec) new ReusableStringCacheIdSnapshotCodec(),
                (CacheEntrySnapshotCodec) new ReusableStringCacheEntrySnapshotCodec(),
                (CacheResponseEncoder) new ReusableStringCacheResponseEncoder(),
                (CacheRequestDecoder) new ReusableStringCacheRequestDecoder());
    }
}
