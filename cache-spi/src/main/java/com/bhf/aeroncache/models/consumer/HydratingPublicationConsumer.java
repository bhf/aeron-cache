package com.bhf.aeroncache.models.consumer;

import com.bhf.aeroncache.annotations.Flyweight;
import lombok.Getter;
import lombok.Setter;
import org.agrona.MutableDirectBuffer;

import java.util.function.Consumer;

/**
 * A flyweight used to pipeline snapshot publications back to a consumer which will publish them.
 */
@Getter
@Setter
@Flyweight
public abstract class HydratingPublicationConsumer implements Consumer<MutableDirectBuffer> {

    int length;
    MutableDirectBuffer buffer;


}
