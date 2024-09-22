/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;


/**
 * A cache has been cleared
 */
@SuppressWarnings("all")
public final class CacheClearedEncoder
{
    public static final int BLOCK_LENGTH = 8;
    public static final int TEMPLATE_ID = 9;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final CacheClearedEncoder parentMessage = this;
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
        return "CacheCleared";
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

    public CacheClearedEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public CacheClearedEncoder wrapAndApplyHeader(
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
        return 8;
    }

    public static String cacheNameMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long cacheNameNullValue()
    {
        return -9223372036854775808L;
    }

    public static long cacheNameMinValue()
    {
        return -9223372036854775807L;
    }

    public static long cacheNameMaxValue()
    {
        return 9223372036854775807L;
    }

    public CacheClearedEncoder cacheName(final long value)
    {
        buffer.putLong(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        final CacheClearedDecoder decoder = new CacheClearedDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
