package com.bhf.aeroncache.services.cache.impl;

import com.bhf.aeroncache.models.results.AddCacheEntryResult;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.UUID;
import java.util.function.Consumer;

@State(Scope.Benchmark)
public class CacheResponseObserversJMH {

    CacheResponseObservers sut;

    @Setup(Level.Trial)
    public void setup() {
        sut = new CacheResponseObservers();
    }

    @Benchmark
    public void addObservers(Blackhole bh) {

        String cacheId = "1L";
        String key = "someKey";
        String value = "someValue";
        String requestId = UUID.randomUUID().toString();
        var consumer = new Consumer<AddCacheEntryResult<ReusableString, ReusableString>>() {
            @Override
            public void accept(AddCacheEntryResult<ReusableString, ReusableString> reusableStringReusableStringAddCacheEntryResult) {

            }
        };

        sut.addCacheEntry(requestId, cacheId, key, value, consumer);

        bh.consume(consumer);
    }


}
