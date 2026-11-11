package com.bhf.aeroncache.services.near;

import com.bhf.aeroncache.events.CacheUpdateEvent.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency regression tests for {@link ConcurrentNearCacheManager}. The near cache is written by the
 * egress/streaming thread (PATCH_ITEM / ADD_ITEM / REMOVE_ITEM) and, in the HTTP near cache, concurrently by
 * request-handler threads (priming {@code put}s, {@code stopNearCaching} removals). These exercise that the
 * manager's compound operations stay atomic under that contention.
 */
@DisplayName("ConcurrentNearCacheManager concurrency")
class ConcurrentNearCacheManagerConcurrencyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CACHE_ID = "concurrentCache";
    private static final String KEY = "k1";

    @Test
    @DisplayName("concurrent PATCH_ITEM deltas on one key do not lose updates")
    void concurrentPatchesDoNotLoseUpdates() throws Exception {
        // Arrange: a warm key holding a baseline, and N threads each poised to merge in a distinct field. A
        // non-atomic get-then-put would let these interleave and clobber one another; the atomic compute must
        // preserve every field.
        int writers = 64;
        var manager = new ConcurrentNearCacheManager<Object>();
        manager.handleCacheUpdate(CACHE_ID, EventType.ADD_ITEM, KEY, "{\"base\":0}");

        var startGate = new CountDownLatch(1);
        var done = new CountDownLatch(writers);
        var failures = new ConcurrentLinkedQueue<Throwable>();
        var pool = Executors.newFixedThreadPool(Math.min(writers, 16));

        // Act: fire every patch at once.
        for (int i = 0; i < writers; i++) {
            final int field = i;
            pool.execute(() -> {
                try {
                    startGate.await();
                    manager.handleCacheUpdate(CACHE_ID, EventType.PATCH_ITEM, KEY, "{\"f" + field + "\":1}");
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }
        startGate.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "patch writers did not finish in time");
        pool.shutdownNow();

        // Assert: no writer threw, and all N fields plus the baseline survived (no lost update).
        assertTrue(failures.isEmpty(), () -> "patch writers threw: " + failures);
        var merged = OBJECT_MAPPER.readTree((String) manager.get(CACHE_ID).get(KEY));
        assertEquals(writers + 1, merged.size(), "every concurrent patch field should survive the merge");
        for (int i = 0; i < writers; i++) {
            assertTrue(merged.has("f" + i), "missing field f" + i + " - a concurrent patch was lost");
        }
    }

    @Test
    @DisplayName("REMOVE_ITEM does not null-deref when the cache is concurrently removed")
    void removeItemDoesNotRaceWithCacheRemoval() throws Exception {
        // Arrange: repeatedly race a streamed REMOVE_ITEM against an admin-style remove(cacheId). A
        // containsKey/get pair would null-deref when the cache is dropped between the two calls.
        int rounds = 5_000;
        var manager = new ConcurrentNearCacheManager<Object>();
        var failure = new AtomicBoolean(false);
        var errors = new ConcurrentLinkedQueue<Throwable>();

        var remover = new Thread(() -> {
            try {
                for (int i = 0; i < rounds; i++) {
                    manager.handleCacheUpdate(CACHE_ID, EventType.REMOVE_ITEM, KEY, null);
                }
            } catch (Throwable t) {
                failure.set(true);
                errors.add(t);
            }
        });

        // Act: concurrently seed then drop the whole cache, forcing the get() in REMOVE_ITEM to observe a
        // mid-flight removal.
        remover.start();
        for (int i = 0; i < rounds; i++) {
            manager.put(CACHE_ID, KEY, "{\"v\":1}");
            manager.remove(CACHE_ID);
        }
        remover.join(TimeUnit.SECONDS.toMillis(30));

        // Assert: the streamed REMOVE_ITEM never threw despite the concurrent cache removal.
        assertTrue(errors.isEmpty(), () -> "REMOVE_ITEM raced with cache removal: " + errors);
        assertTrue(!failure.get(), "REMOVE_ITEM threw under concurrent cache removal");
    }
}
