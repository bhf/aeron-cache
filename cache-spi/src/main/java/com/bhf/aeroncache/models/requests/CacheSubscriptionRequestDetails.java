package com.bhf.aeroncache.models.requests;

import com.bhf.aeroncache.annotations.Flyweight;
import com.bhf.aeroncache.models.RequestId;
import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.pool.DequeReusableObjectPool;
import com.bhf.aeroncache.pool.ReusableObjectPool;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
@Flyweight
public class CacheSubscriptionRequestDetails<I extends Reusable, K extends Reusable> implements Reusable<CacheSubscriptionRequestDetails<I, K>> {

    private static final int SUBSCRIPTION_ITEM_POOL_INITIAL_SIZE = 16;

    final RequestId requestId = new RequestId();
    final List<I> cacheId = new ArrayList<>();
    final List<K> subscriptionKey = new ArrayList<>();
    final List<SubscriptionMode> subscriptionMode = new ArrayList<>();
    boolean sendSnapshot;

    private final ReusableObjectPool<I> cacheIdPool;
    private final ReusableObjectPool<K> subscriptionKeyPool;

    /**
     * Create a request details flyweight backed by pools that reuse the cache id and key instances
     * gathered while decoding.
     *
     * @param cacheIdSupplier Factory for cache id instances.
     * @param keySupplier     Factory for subscription key instances.
     */
    public CacheSubscriptionRequestDetails(final Supplier<I> cacheIdSupplier, final Supplier<K> keySupplier) {
        this.cacheIdPool = new DequeReusableObjectPool<>(cacheIdSupplier, SUBSCRIPTION_ITEM_POOL_INITIAL_SIZE, true);
        this.subscriptionKeyPool = new DequeReusableObjectPool<>(keySupplier, SUBSCRIPTION_ITEM_POOL_INITIAL_SIZE, true);
    }

    public String getRequestId(){
        return requestId.getRequestId();
    }

    public void setRequestId(String requestId) {
        this.requestId.setRequestId(requestId);
    }

    /**
     * Add a single subscription item, acquiring the backing cache id (and key, when present) from
     * the pools rather than allocating fresh instances. A null or empty {@code keyValue} records a
     * whole-cache subscription with a {@code null} key.
     *
     * @param cacheIdValue The cache id to subscribe on.
     * @param keyValue     The key to subscribe on, or null/empty for a whole-cache subscription.
     * @param mode         The subscription mode.
     */
    public void addSubscription(final String cacheIdValue, final String keyValue, final SubscriptionMode mode) {
        final I pooledCacheId = cacheIdPool.acquire();
        pooledCacheId.copyFrom(cacheIdValue);
        cacheId.add(pooledCacheId);

        if (keyValue == null || keyValue.isEmpty()) {
            subscriptionKey.add(null);
        } else {
            final K pooledKey = subscriptionKeyPool.acquire();
            pooledKey.copyFrom(keyValue);
            subscriptionKey.add(pooledKey);
        }

        subscriptionMode.add(mode);
    }

    /**
     * Return the pooled cache id and key instances gathered by {@link #addSubscription} back to
     * their pools and clear the item lists, leaving the flyweight ready for the next request. The
     * request id and snapshot flag are left untouched.
     */
    public void recycle() {
        for (I value : cacheId) {
            cacheIdPool.release(value);
        }
        for (final K key : subscriptionKey) {
            if (key != null) {
                subscriptionKeyPool.release(key);
            }
        }
        cacheId.clear();
        subscriptionKey.clear();
        subscriptionMode.clear();
    }

    @Override
    public void clear() {
        requestId.clear();
        cacheId.clear();
        subscriptionKey.clear();
        subscriptionMode.clear();
    }

    @Override
    public void copyFrom(CacheSubscriptionRequestDetails<I, K> source) {
        this.requestId.copyFrom(source.requestId);
        this.cacheId.addAll(source.cacheId);
        this.subscriptionKey.addAll(source.subscriptionKey);
        this.subscriptionMode.addAll(source.subscriptionMode);
    }

    @Override
    public void copyFrom(Reusable<CacheSubscriptionRequestDetails<I, K>> source) {
        this.copyFrom(source.value());
    }

    @Override
    public CacheSubscriptionRequestDetails<I, K> value() {
        return this;
    }
}
