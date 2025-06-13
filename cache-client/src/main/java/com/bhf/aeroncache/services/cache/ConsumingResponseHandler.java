package com.bhf.aeroncache.services.cache;

import com.bhf.aeroncache.models.results.*;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;

import java.util.function.Consumer;

public interface ConsumingResponseHandler extends CacheRequestConsumingPublisher, CacheResponseHandler{
    void setCreateCacheConsumer(Consumer<CreateCacheResult<ReusableLong>> c);

    void setAddCacheEntryConsumer(Consumer<AddCacheEntryResult<ReusableLong, ReusableString>> c);

    void setClearCacheConsumer(Consumer<ClearCacheResult<ReusableLong>> c);

    void setDeleteCacheConsumer(Consumer<DeleteCacheResult<ReusableLong>> c);

    void setRemoveCacheEntryConsumer(Consumer<RemoveCacheEntryResult<ReusableLong, ReusableString>> c);

    void setGetCacheEntryConsumer(Consumer<GetCacheEntryResult<ReusableLong, ReusableString, ReusableString>> c);
}
