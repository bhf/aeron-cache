package com.bhf.aeroncache.types;

import com.bhf.aeroncache.models.Reusable;
import lombok.Getter;

@Getter
public class ReusableLong implements Reusable<Long> {

    long value;

    @Override
    public void clear() {
        value = 0;
    }

    @Override
    public void copyFrom(Long source) {
        value = source;
    }

    @Override
    public void copyFrom(Reusable<Long> source) {
        this.copyFrom(source.value());
    }

    @Override
    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Long && value == (long) obj ||
                obj instanceof ReusableLong && ((ReusableLong)obj).value().equals(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }
}
