package com.bhf.aeroncache.utils;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.function.Supplier;

/**
 * Resolves {@link IdleStrategy} suppliers from configuration with a fallback.
 * <p>
 * An idle strategy is selected by a short specification string using the Agrona strategy aliases:
 * {@code backoff}, {@code spin}, {@code noop}, {@code yield}, {@code sleep-ns} and {@code sleep-ms}.
 * The two sleeping strategies accept an optional period argument after a colon, e.g.
 * {@code sleep-ns:1000} or {@code sleep-ms:1}. Any other value is treated as a fully qualified class
 * name of an {@link IdleStrategy} implementation with a public no-arg constructor.
 * <p>
 * {@link IdleStrategy} implementations are stateful and must not be shared across threads, so the
 * returned {@link Supplier} constructs a fresh instance on every {@link Supplier#get()}.
 */
public final class IdleStrategyConfig {

    /**
     * Application wide default environment variable, used when a role specific variable is not set.
     */
    public static final String IDLE_STRATEGY_ENV = "IDLE_STRATEGY";

    private IdleStrategyConfig() {
    }

    /**
     * Build a supplier that mints a fresh {@link IdleStrategy} per call, chosen from the first of
     * {@code envVars} that is set to a non-blank value, falling back to {@code fallback} when none
     * are set.
     * <p>
     * The configured value is validated eagerly so an invalid specification fails fast at startup
     * rather than when the first agent is created.
     *
     * @param fallback the supplier to use when no environment variable is set (preserves existing behaviour).
     * @param envVars  the environment variable names to check, in priority order (role specific first).
     * @return a supplier of fresh idle strategies.
     */
    public static Supplier<IdleStrategy> supplier(final Supplier<IdleStrategy> fallback, final String... envVars) {
        final String spec = firstSet(envVars);
        if (null == spec) {
            return fallback;
        }

        // Validate eagerly so a bad value surfaces at startup, then construct fresh per get().
        parse(spec);
        return () -> parse(spec);
    }

    private static String firstSet(final String... envVars) {
        for (final String envVar : envVars) {
            final String value = System.getenv(envVar);
            if (null != value && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Parse a strategy specification into a new {@link IdleStrategy} instance.
     *
     * @param spec the specification string, e.g. {@code backoff}, {@code sleep-ns:1000} or a class name.
     * @return a new idle strategy instance.
     */
    static IdleStrategy parse(final String spec) {
        final int colon = spec.indexOf(':');
        final String alias = (colon < 0 ? spec : spec.substring(0, colon)).trim();
        final String arg = colon < 0 ? null : spec.substring(colon + 1).trim();

        switch (alias) {
            case BackoffIdleStrategy.ALIAS:
                return new BackoffIdleStrategy();
            case BusySpinIdleStrategy.ALIAS:
                return new BusySpinIdleStrategy();
            case NoOpIdleStrategy.ALIAS:
                return new NoOpIdleStrategy();
            case YieldingIdleStrategy.ALIAS:
                return new YieldingIdleStrategy();
            case SleepingIdleStrategy.ALIAS:
                return null == arg ? new SleepingIdleStrategy() : new SleepingIdleStrategy(parsePeriod(spec, arg));
            case SleepingMillisIdleStrategy.ALIAS:
                return null == arg ? new SleepingMillisIdleStrategy() : new SleepingMillisIdleStrategy(parsePeriod(spec, arg));
            default:
                return newFromClassName(spec);
        }
    }

    private static long parsePeriod(final String spec, final String arg) {
        try {
            return Long.parseLong(arg);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid idle strategy period in '" + spec + "'", e);
        }
    }

    private static IdleStrategy newFromClassName(final String className) {
        try {
            final Class<?> clazz = Class.forName(className);
            return (IdleStrategy) clazz.getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException | ClassCastException e) {
            throw new IllegalArgumentException("Unknown idle strategy specification: '" + className + "'", e);
        }
    }
}
