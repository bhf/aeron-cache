package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.UUID;
import java.util.function.Consumer;

@State(Scope.Benchmark)
public class CacheResponseMapObserversJMH {

    CacheResponseMapObservers sut;

    @Setup(Level.Trial)
    public void setup() {
        sut = new CacheResponseMapObservers();
    }

    @Benchmark
    public void addObservers(Blackhole bh) {

        long cacheId = 1L;
        String key = "someKey";
        String value = "someValue";
        String requestId = UUID.randomUUID().toString();
        var consumer = new Consumer<AddCacheEntryResult<ReusableLong, ReusableString>>() {
            @Override
            public void accept(AddCacheEntryResult<ReusableLong, ReusableString> reusableLongReusableStringAddCacheEntryResult) {

            }
        };

        sut.addCacheEntry(cacheId, key, value, consumer, requestId);

        bh.consume(consumer);
    }


}
