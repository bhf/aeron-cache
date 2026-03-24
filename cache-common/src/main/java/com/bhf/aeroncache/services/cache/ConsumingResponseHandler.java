package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.*;

import java.util.function.Consumer;

public interface ConsumingResponseHandler<I extends Reusable, K extends Reusable, V extends Reusable> extends CacheRequestConsumingPublisher<I, K, V>, CacheResponseHandler<I, K, V> {
    void setCreateCacheConsumer(Consumer<CreateCacheResult<I>> c);

    void setAddCacheEntryConsumer(Consumer<AddCacheEntryResult<I, K>> c);

    void setClearCacheConsumer(Consumer<ClearCacheResult<I>> c);

    void setDeleteCacheConsumer(Consumer<DeleteCacheResult<I>> c);

    void setRemoveCacheEntryConsumer(Consumer<RemoveCacheEntryResult<I, K>> c);

    void setGetCacheEntryConsumer(Consumer<GetCacheEntryResult<I, K, V>> c);
}
