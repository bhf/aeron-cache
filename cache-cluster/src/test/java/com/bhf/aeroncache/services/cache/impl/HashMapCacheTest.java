package com.bhf.aeroncache.services.cache.impl;

import org.junit.jupiter.api.BeforeEach;

class HashMapCacheTest {

    HashMapCache<Long, String, String> cache;

    @BeforeEach
    void setup(){
        cache=new HashMapCache<>();
    }



}
