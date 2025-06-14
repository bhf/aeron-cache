package com.bhf.aeroncache.encoders;

import com.bhf.aeroncache.messages.CreateCacheEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import org.agrona.MutableDirectBuffer;

public class CacheRequestEncoder {

    public static int encodeCreateCacheRequest(CreateCacheEncoder encoder, MessageHeaderEncoder headerEncoder, MutableDirectBuffer msgBuffer, String requestId, long cacheId) {
        encoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder)
                .cacheId(cacheId)
                .requestId(requestId);

        return encoder.encodedLength()+ headerEncoder.encodedLength();
    }
}
