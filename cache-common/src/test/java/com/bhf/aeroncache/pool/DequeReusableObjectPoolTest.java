package com.bhf.aeroncache.pool;

import com.bhf.aeroncache.models.ReusableLong;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DequeReusableObjectPoolTest {

    private final Supplier<ReusableLong> factory = ReusableLong::new;

    @Test
    void shouldPrePopulateWithInitialSize() {
        // Arrange & Act
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 3, false);

        // Assert
        assertEquals(3, pool.available());
    }

    @Test
    void shouldAcquireFromPoolReducingAvailability() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 2, false);

        // Act
        final ReusableLong acquired = pool.acquire();

        // Assert
        assertNotNull(acquired);
        assertEquals(1, pool.available());
    }

    @Test
    void shouldCreateNewInstanceWhenPoolEmpty() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 0, false);

        // Act
        final ReusableLong acquired = pool.acquire();

        // Assert
        assertEquals(0, pool.available());
        assertNotNull(acquired);
    }

    @Test
    void shouldReuseReleasedInstance() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 1, false);
        final ReusableLong first = pool.acquire();

        // Act
        pool.release(first);
        final ReusableLong reacquired = pool.acquire();

        // Assert
        assertSame(first, reacquired);
    }

    @Test
    void shouldClearInstanceOnRelease() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 1, false);
        final ReusableLong instance = pool.acquire();
        instance.copyFrom(42L);

        // Act
        pool.release(instance);

        // Assert
        assertEquals(0L, pool.acquire().value());
    }

    @Test
    void shouldNotGrowBeyondInitialSizeWhenRecycleDisabled() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 1, false);
        final ReusableLong pooled = pool.acquire();
        final ReusableLong onDemand = pool.acquire();

        // Act
        pool.release(pooled);
        pool.release(onDemand);

        // Assert - only the initial capacity is retained; the extra instance is discarded.
        assertEquals(1, pool.available());
    }

    @Test
    void shouldGrowBeyondInitialSizeWhenRecycleEnabled() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 1, true);
        final ReusableLong pooled = pool.acquire();
        final ReusableLong onDemand = pool.acquire();

        // Act
        pool.release(pooled);
        pool.release(onDemand);

        // Assert - both instances are retained, growing the pool beyond its initial size.
        assertEquals(2, pool.available());
    }

    @Test
    void shouldIgnoreNullRelease() {
        // Arrange
        final DequeReusableObjectPool<ReusableLong> pool =
                new DequeReusableObjectPool<>(factory, 0, true);

        // Act
        pool.release(null);

        // Assert
        assertEquals(0, pool.available());
    }

    @Test
    void shouldRejectNullFactory() {
        // Arrange, Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DequeReusableObjectPool<ReusableLong>(null, 1, false));
    }

    @Test
    void shouldRejectNegativeInitialSize() {
        // Arrange, Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new DequeReusableObjectPool<>(factory, -1, false));
    }
}
