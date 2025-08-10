package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableString;

import java.util.function.Consumer;

public interface ConsumingResponseHandler extends CacheRequestConsumingPublisher, CacheResponseHandler{
    void setCreateCacheConsumer(Consumer<CreateCacheResult<ReusableString>> c);

    void setAddCacheEntryConsumer(Consumer<AddCacheEntryResult<ReusableString, ReusableString>> c);

    void setClearCacheConsumer(Consumer<ClearCacheResult<ReusableString>> c);

    void setDeleteCacheConsumer(Consumer<DeleteCacheResult<ReusableString>> c);

    void setRemoveCacheEntryConsumer(Consumer<RemoveCacheEntryResult<ReusableString, ReusableString>> c);

    void setGetCacheEntryConsumer(Consumer<GetCacheEntryResult<ReusableString, ReusableString, ReusableString>> c);
}
