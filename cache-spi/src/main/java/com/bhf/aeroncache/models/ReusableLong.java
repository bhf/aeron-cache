package com.bhf.aeroncache.models;

import lombok.Getter;

@Getter
public class ReusableLong implements Reusable<Long> {

    Long value;

    @Override
    public void clear() {
        value = 0L;
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
        return value.toString();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public long increment(long amount) {
        value+=amount;
        return value;
    }

    public long decrement(long amount) {
        value-=amount;
        return value;
    }
}
