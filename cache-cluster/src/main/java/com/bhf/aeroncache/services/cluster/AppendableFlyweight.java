package com.bhf.aeroncache.services.cluster;

import com.bhf.aeroncache.types.ReusableString;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

public class AppendableFlyweight implements Appendable{

    @Getter
    @Setter
    ReusableString reusable;

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        reusable.copyFrom(csq);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        reusable.append(c);
        return this;
    }
}
