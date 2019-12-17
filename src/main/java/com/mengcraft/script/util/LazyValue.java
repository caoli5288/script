package com.mengcraft.script.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyValue<T> {

    @NonNull
    private final Supplier<T> factory;
    private T obj;

    public T get() {
        if (obj == null) obj = Objects.requireNonNull(factory.get(), "lazy load value");
        return obj;
    }
}
