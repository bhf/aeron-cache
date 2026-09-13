package com.bhf.aeroncache.utils;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleStrategyConfigTest {

    @ParameterizedTest
    @CsvSource({
            "backoff,org.agrona.concurrent.BackoffIdleStrategy",
            "spin,org.agrona.concurrent.BusySpinIdleStrategy",
            "noop,org.agrona.concurrent.NoOpIdleStrategy",
            "yield,org.agrona.concurrent.YieldingIdleStrategy",
            "sleep-ns,org.agrona.concurrent.SleepingIdleStrategy",
            "sleep-ms,org.agrona.concurrent.SleepingMillisIdleStrategy"
    })
    void parsesEachAliasToTheExpectedType(final String alias, final String expectedClassName) {
        assertEquals(expectedClassName, IdleStrategyConfig.parse(alias).getClass().getName());
    }

    @Test
    void trimsSurroundingWhitespaceInTheSpec() {
        assertTrue(IdleStrategyConfig.parse("  spin  ") instanceof BusySpinIdleStrategy);
    }

    @Test
    void parsesSleepingStrategiesWithAndWithoutAPeriodArgument() {
        assertTrue(IdleStrategyConfig.parse("sleep-ns") instanceof SleepingIdleStrategy);
        assertTrue(IdleStrategyConfig.parse("sleep-ns:5000") instanceof SleepingIdleStrategy);
        assertTrue(IdleStrategyConfig.parse("sleep-ms") instanceof SleepingMillisIdleStrategy);
        assertTrue(IdleStrategyConfig.parse("sleep-ms:2") instanceof SleepingMillisIdleStrategy);
    }

    @Test
    void parsesAFullyQualifiedClassName() {
        assertTrue(IdleStrategyConfig.parse("org.agrona.concurrent.NoOpIdleStrategy") instanceof NoOpIdleStrategy);
    }

    @Test
    void rejectsAnUnknownSpecification() {
        assertThrows(IllegalArgumentException.class, () -> IdleStrategyConfig.parse("not-a-strategy"));
    }

    @Test
    void rejectsANonNumericSleepingPeriod() {
        assertThrows(IllegalArgumentException.class, () -> IdleStrategyConfig.parse("sleep-ns:fast"));
    }

    @Test
    void mintsAFreshInstanceEachTimeSoStateIsNotSharedAcrossThreads() {
        assertNotSame(IdleStrategyConfig.parse("backoff"), IdleStrategyConfig.parse("backoff"));
    }

    @Test
    void usesTheFallbackWhenNoEnvironmentVariableIsSet() {
        final Supplier<IdleStrategy> fallback = BackoffIdleStrategy::new;
        // No env var names supplied, so nothing is set and the fallback is returned as-is.
        assertSame(fallback, IdleStrategyConfig.supplier(fallback));
    }

    @Test
    void fallbackSupplierAlsoMintsFreshInstances() {
        final Supplier<IdleStrategy> supplier = IdleStrategyConfig.supplier(BackoffIdleStrategy::new);
        assertNotSame(supplier.get(), supplier.get());
    }
}
