package com.bhf.aeroncache.types;

import com.bhf.aeroncache.models.Reusable;

public class ReusableString implements Reusable<String> {

    final StringBuilder sb = new StringBuilder();

    @Override
    public void clear() {
        sb.setLength(0);
    }

    @Override
    public void copyFrom(String source) {
        sb.append(source);
    }

    @Override
    public void copyFrom(Reusable<String> source) {
        this.copyFrom(source.value());
    }

    public void copyFrom(ReusableString source) {
        sb.append(source.sb);
    }

    public void copyFrom(CharSequence source) {
        sb.append(source);
    }

    public void copyFrom(StringBuilder source) {
        sb.append(source);
    }

    @Override
    public String value(){
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReusableString && sb.toString().equals(obj.toString()) ||
                obj instanceof StringBuilder && sb.toString().equals(obj.toString()) ||
                obj instanceof String && sb.toString().equals(obj.toString());
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return sb.toString().hashCode();
    }

    public void append(char c) {
        sb.append(c);
    }
}
