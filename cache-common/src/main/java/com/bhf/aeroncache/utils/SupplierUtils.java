package com.bhf.aeroncache.utils;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.types.ReusableLong;
import com.bhf.aeroncache.types.ReusableString;
import org.agrona.collections.Object2ObjectHashMap;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Static {@link Supplier} instances to help create {@link Reusable} instances.
 */
public class SupplierUtils {
    public static final Supplier<ReusableLong> longSupplier = ReusableLong::new;
    public static final Supplier<ReusableString> stringSupplier = ReusableString::new;
    public static Supplier<Map<ReusableString, ReusableString>> mapSupplier = Object2ObjectHashMap::new;
}
