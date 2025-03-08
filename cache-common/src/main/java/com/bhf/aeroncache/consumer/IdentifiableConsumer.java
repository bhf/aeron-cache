package com.bhf.aeroncache.consumer;

import java.util.function.Consumer;

/**
 * A wrapper around a {@link Consumer} that allows it to become
 * identifiable.
 * @param <T> The type which the {@link Consumer} consumes.
 * @param <I> The type by which the {@link Consumer} is identifiable.
 */
public interface IdentifiableConsumer<I,T> extends Consumer<T> {
    I getId();
}
