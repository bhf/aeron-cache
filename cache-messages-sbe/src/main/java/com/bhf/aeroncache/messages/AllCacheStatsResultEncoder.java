/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;


/**
 * All of the cache stats
 */
@SuppressWarnings("all")
public final class AllCacheStatsResultEncoder
{
    public static final int BLOCK_LENGTH = 1;
    public static final int TEMPLATE_ID = 16;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final AllCacheStatsResultEncoder parentMessage = this;
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
        return "AllCacheStatsResult";
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

    public AllCacheStatsResultEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public AllCacheStatsResultEncoder wrapAndApplyHeader(
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

    public static int statusId()
    {
        return 1;
    }

    public static int statusSinceVersion()
    {
        return 0;
    }

    public static int statusEncodingOffset()
    {
        return 0;
    }

    public static int statusEncodingLength()
    {
        return 1;
    }

    public static String statusMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public AllCacheStatsResultEncoder status(final OperationStatus value)
    {
        buffer.putByte(offset + 0, (byte)value.value());
        return this;
    }

    private final StatsEncoder stats = new StatsEncoder(this);

    public static long statsId()
    {
        return 20;
    }

    public StatsEncoder statsCount(final int count)
    {
        stats.wrap(buffer, count);
        return stats;
    }

    public static final class StatsEncoder
    {
        public static final int HEADER_SIZE = 4;
        private final AllCacheStatsResultEncoder parentMessage;
        private MutableDirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int initialLimit;

        StatsEncoder(final AllCacheStatsResultEncoder parentMessage)
        {
            this.parentMessage = parentMessage;
        }

        public void wrap(final MutableDirectBuffer buffer, final int count)
        {
            if (count < 0 || count > 65534)
            {
                throw new IllegalArgumentException("count outside allowed range: count=" + count);
            }

            if (buffer != this.buffer)
            {
                this.buffer = buffer;
            }

            index = 0;
            this.count = count;
            final int limit = parentMessage.limit();
            initialLimit = limit;
            parentMessage.limit(limit + HEADER_SIZE);
            buffer.putShort(limit + 0, (short)40, java.nio.ByteOrder.LITTLE_ENDIAN);
            buffer.putShort(limit + 2, (short)count, java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        public StatsEncoder next()
        {
            if (index >= count)
            {
                throw new java.util.NoSuchElementException();
            }

            offset = parentMessage.limit();
            parentMessage.limit(offset + sbeBlockLength());
            ++index;

            return this;
        }

        public int resetCountToIndex()
        {
            count = index;
            buffer.putShort(initialLimit + 2, (short)count, java.nio.ByteOrder.LITTLE_ENDIAN);

            return count;
        }

        public static int countMinValue()
        {
            return 0;
        }

        public static int countMaxValue()
        {
            return 65534;
        }

        public static int sbeHeaderSize()
        {
            return HEADER_SIZE;
        }

        public static int sbeBlockLength()
        {
            return 40;
        }

        public static int addedId()
        {
            return 21;
        }

        public static int addedSinceVersion()
        {
            return 0;
        }

        public static int addedEncodingOffset()
        {
            return 0;
        }

        public static int addedEncodingLength()
        {
            return 8;
        }

        public static String addedMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long addedNullValue()
        {
            return -9223372036854775808L;
        }

        public static long addedMinValue()
        {
            return -9223372036854775807L;
        }

        public static long addedMaxValue()
        {
            return 9223372036854775807L;
        }

        public StatsEncoder added(final long value)
        {
            buffer.putLong(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static int removedId()
        {
            return 22;
        }

        public static int removedSinceVersion()
        {
            return 0;
        }

        public static int removedEncodingOffset()
        {
            return 8;
        }

        public static int removedEncodingLength()
        {
            return 8;
        }

        public static String removedMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long removedNullValue()
        {
            return -9223372036854775808L;
        }

        public static long removedMinValue()
        {
            return -9223372036854775807L;
        }

        public static long removedMaxValue()
        {
            return 9223372036854775807L;
        }

        public StatsEncoder removed(final long value)
        {
            buffer.putLong(offset + 8, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static int clearedId()
        {
            return 23;
        }

        public static int clearedSinceVersion()
        {
            return 0;
        }

        public static int clearedEncodingOffset()
        {
            return 16;
        }

        public static int clearedEncodingLength()
        {
            return 8;
        }

        public static String clearedMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long clearedNullValue()
        {
            return -9223372036854775808L;
        }

        public static long clearedMinValue()
        {
            return -9223372036854775807L;
        }

        public static long clearedMaxValue()
        {
            return 9223372036854775807L;
        }

        public StatsEncoder cleared(final long value)
        {
            buffer.putLong(offset + 16, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static int sizeId()
        {
            return 24;
        }

        public static int sizeSinceVersion()
        {
            return 0;
        }

        public static int sizeEncodingOffset()
        {
            return 24;
        }

        public static int sizeEncodingLength()
        {
            return 8;
        }

        public static String sizeMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long sizeNullValue()
        {
            return -9223372036854775808L;
        }

        public static long sizeMinValue()
        {
            return -9223372036854775807L;
        }

        public static long sizeMaxValue()
        {
            return 9223372036854775807L;
        }

        public StatsEncoder size(final long value)
        {
            buffer.putLong(offset + 24, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }


        public static int cacheIdId()
        {
            return 25;
        }

        public static int cacheIdSinceVersion()
        {
            return 0;
        }

        public static int cacheIdEncodingOffset()
        {
            return 32;
        }

        public static int cacheIdEncodingLength()
        {
            return 8;
        }

        public static String cacheIdMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long cacheIdNullValue()
        {
            return -9223372036854775808L;
        }

        public static long cacheIdMinValue()
        {
            return -9223372036854775807L;
        }

        public static long cacheIdMaxValue()
        {
            return 9223372036854775807L;
        }

        public StatsEncoder cacheId(final long value)
        {
            buffer.putLong(offset + 32, value, java.nio.ByteOrder.LITTLE_ENDIAN);
            return this;
        }

    }

    public static int requestIdId()
    {
        return 3;
    }

    public static String requestIdCharacterEncoding()
    {
        return "UTF-8";
    }

    public static String requestIdMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static int requestIdHeaderLength()
    {
        return 4;
    }

    public AllCacheStatsResultEncoder putRequestId(final DirectBuffer src, final int srcOffset, final int length)
    {
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putBytes(limit + headerLength, src, srcOffset, length);

        return this;
    }

    public AllCacheStatsResultEncoder putRequestId(final byte[] src, final int srcOffset, final int length)
    {
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putBytes(limit + headerLength, src, srcOffset, length);

        return this;
    }

    public AllCacheStatsResultEncoder requestId(final String value)
    {
        final byte[] bytes;
        try
        {
            bytes = null == value || value.isEmpty() ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes("UTF-8");
        }
        catch (final java.io.UnsupportedEncodingException ex)
        {
            throw new RuntimeException(ex);
        }

        final int length = bytes.length;
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putBytes(limit + headerLength, bytes, 0, length);

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

        final AllCacheStatsResultDecoder decoder = new AllCacheStatsResultDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
