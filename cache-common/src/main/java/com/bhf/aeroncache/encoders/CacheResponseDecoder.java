package com.bhf.aeroncache.encoders;

import com.bhf.aeroncache.messages.CacheCreatedDecoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import com.bhf.aeroncache.models.results.CreateCacheResult;
import com.bhf.aeroncache.types.ReusableLong;
import org.agrona.DirectBuffer;

public class CacheResponseDecoder {

    public static void decodeCacheCreated(CreateCacheResult<ReusableLong> createCacheResult, CacheCreatedDecoder cacheCreatedDecoder, MessageHeaderDecoder headerDecoder, DirectBuffer buffer, int offset){
        cacheCreatedDecoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
        var cacheId = cacheCreatedDecoder.cacheId();
        var requestId = cacheCreatedDecoder.requestId();
        var status = cacheCreatedDecoder.status();

        createCacheResult.clear();
        createCacheResult.getCacheId().copyFrom(cacheId);
        createCacheResult.setRequestId(requestId);
        createCacheResult.setStatus(status);
    }
}
