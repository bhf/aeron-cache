package com.bhf.aeroncache.application;

import com.bhf.aeroncache.services.cache.snapshot.CacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.CacheIdSnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheEntrySnapshotCodec;
import com.bhf.aeroncache.services.cache.snapshot.ReusableStringCacheIdSnapshotCodec;
import com.bhf.aeroncache.types.ReusableString;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CacheSnapshotCodecUtils {

    public static CacheEntrySnapshotCodec<ReusableString, ReusableString> getCacheEntrySnapshotCodec() {
        return new ReusableStringCacheEntrySnapshotCodec();
    }

    public static CacheIdSnapshotCodec<ReusableString> getCacheIdSnapshotCodec() {
        return new ReusableStringCacheIdSnapshotCodec();
    }
}
