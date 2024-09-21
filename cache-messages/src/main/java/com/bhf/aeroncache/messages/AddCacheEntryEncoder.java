/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;


/**
 * Add an entry too a cache
 */
@SuppressWarnings("all")
public final class AddCacheEntryEncoder
{
    public static final int BLOCK_LENGTH = 20;
    public static final int TEMPLATE_ID = 4;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final AddCacheEntryEncoder parentMessage = this;
    private MutableDirectBuffer buffer;
    private int initialOffset;
    private int offset;
    private int limit;

    public int sbeBlockLength()
    {
        return BLOCK_LENGTH;
    }

    public int sbeTemplateId()
    {
        return TEMPLATE_ID;
    }

    public int sbeSchemaId()
    {
        return SCHEMA_ID;
    }

    public int sbeSchemaVersion()
    {
        return SCHEMA_VERSION;
    }

    public String sbeSemanticType()
    {
        return "AddCacheEntry";
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int initialOffset()
    {
        return initialOffset;
    }

    public int offset()
    {
        return offset;
    }

    public AddCacheEntryEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.initialOffset = offset;
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public AddCacheEntryEncoder wrapAndApplyHeader(
        final MutableDirectBuffer buffer, final int offset, final MessageHeaderEncoder headerEncoder)
    {
        headerEncoder
            .wrap(buffer, offset)
            .blockLength(BLOCK_LENGTH)
            .templateId(TEMPLATE_ID)
            .schemaId(SCHEMA_ID)
            .version(SCHEMA_VERSION);

        return wrap(buffer, offset + MessageHeaderEncoder.ENCODED_LENGTH);
    }

    public int encodedLength()
    {
        return limit - offset;
    }

    public int limit()
    {
        return limit;
    }

    public void limit(final int limit)
    {
        this.limit = limit;
    }

    public static int cacheNameId()
    {
        return 1;
    }

    public static int cacheNameSinceVersion()
    {
        return 0;
    }

    public static int cacheNameEncodingOffset()
    {
        return 0;
    }

    public static int cacheNameEncodingLength()
    {
        return 4;
    }

    public static String cacheNameMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    private final VarStringEncodingEncoder cacheName = new VarStringEncodingEncoder();

    public VarStringEncodingEncoder cacheName()
    {
        cacheName.wrap(buffer, offset + 0);
        return cacheName;
    }

    public static int keyId()
    {
        return 2;
    }

    public static int keySinceVersion()
    {
        return 0;
    }

    public static int keyEncodingOffset()
    {
        return 4;
    }

    public static int keyEncodingLength()
    {
        return 8;
    }

    public static String keyMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long keyNullValue()
    {
        return -9223372036854775808L;
    }

    public static long keyMinValue()
    {
        return -9223372036854775807L;
    }

    public static long keyMaxValue()
    {
        return 9223372036854775807L;
    }

    public AddCacheEntryEncoder key(final long value)
    {
        buffer.putLong(offset + 4, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }


    public static int valueId()
    {
        return 3;
    }

    public static int valueSinceVersion()
    {
        return 0;
    }

    public static int valueEncodingOffset()
    {
        return 12;
    }

    public static int valueEncodingLength()
    {
        return 8;
    }

    public static String valueMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long valueNullValue()
    {
        return -9223372036854775808L;
    }

    public static long valueMinValue()
    {
        return -9223372036854775807L;
    }

    public static long valueMaxValue()
    {
        return 9223372036854775807L;
    }

    public AddCacheEntryEncoder value(final long value)
    {
        buffer.putLong(offset + 12, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }


    public String toString()
    {
        if (null == buffer)
        {
            return "";
        }

        return appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        if (null == buffer)
        {
            return builder;
        }

        final AddCacheEntryDecoder decoder = new AddCacheEntryDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
