/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;


/**
 * All of the cache stats
 */
@SuppressWarnings("all")
public final class AllCacheStatsResultDecoder
{
    public static final int BLOCK_LENGTH = 1;
    public static final int TEMPLATE_ID = 16;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final AllCacheStatsResultDecoder parentMessage = this;
    private DirectBuffer buffer;
    private int initialOffset;
    private int offset;
    private int limit;
    int actingBlockLength;
    int actingVersion;

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

    public DirectBuffer buffer()
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

    public AllCacheStatsResultDecoder wrap(
        final DirectBuffer buffer,
        final int offset,
        final int actingBlockLength,
        final int actingVersion)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.initialOffset = offset;
        this.offset = offset;
        this.actingBlockLength = actingBlockLength;
        this.actingVersion = actingVersion;
        limit(offset + actingBlockLength);

        return this;
    }

    public AllCacheStatsResultDecoder wrapAndApplyHeader(
        final DirectBuffer buffer,
        final int offset,
        final MessageHeaderDecoder headerDecoder)
    {
        headerDecoder.wrap(buffer, offset);

        final int templateId = headerDecoder.templateId();
        if (TEMPLATE_ID != templateId)
        {
            throw new IllegalStateException("Invalid TEMPLATE_ID: " + templateId);
        }

        return wrap(
            buffer,
            offset + MessageHeaderDecoder.ENCODED_LENGTH,
            headerDecoder.blockLength(),
            headerDecoder.version());
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

    public short statusRaw()
    {
        return ((short)(buffer.getByte(offset + 0) & 0xFF));
    }

    public OperationStatus status()
    {
        return OperationStatus.get(((short)(buffer.getByte(offset + 0) & 0xFF)));
    }


    private final StatsDecoder stats = new StatsDecoder(this);

    public static long statsDecoderId()
    {
        return 20;
    }

    public static int statsDecoderSinceVersion()
    {
        return 0;
    }

    public StatsDecoder stats()
    {
        stats.wrap(buffer);
        return stats;
    }

    public static final class StatsDecoder
        implements Iterable<StatsDecoder>, java.util.Iterator<StatsDecoder>
    {
        public static final int HEADER_SIZE = 4;
        private final AllCacheStatsResultDecoder parentMessage;
        private DirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int blockLength;

        StatsDecoder(final AllCacheStatsResultDecoder parentMessage)
        {
            this.parentMessage = parentMessage;
        }

        public void wrap(final DirectBuffer buffer)
        {
            if (buffer != this.buffer)
            {
                this.buffer = buffer;
            }

            index = 0;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + HEADER_SIZE);
            blockLength = (buffer.getShort(limit + 0, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
            count = (buffer.getShort(limit + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF);
        }

        public StatsDecoder next()
        {
            if (index >= count)
            {
                throw new java.util.NoSuchElementException();
            }

            offset = parentMessage.limit();
            parentMessage.limit(offset + blockLength);
            ++index;

            return this;
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

        public int actingBlockLength()
        {
            return blockLength;
        }

        public int count()
        {
            return count;
        }

        public java.util.Iterator<StatsDecoder> iterator()
        {
            return this;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext()
        {
            return index < count;
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

        public long added()
        {
            return buffer.getLong(offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        public long removed()
        {
            return buffer.getLong(offset + 8, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        public long cleared()
        {
            return buffer.getLong(offset + 16, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        public long size()
        {
            return buffer.getLong(offset + 24, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        public long cacheId()
        {
            return buffer.getLong(offset + 32, java.nio.ByteOrder.LITTLE_ENDIAN);
        }


        public StringBuilder appendTo(final StringBuilder builder)
        {
            if (null == buffer)
            {
                return builder;
            }

            builder.append('(');
            builder.append("added=");
            builder.append(added());
            builder.append('|');
            builder.append("removed=");
            builder.append(removed());
            builder.append('|');
            builder.append("cleared=");
            builder.append(cleared());
            builder.append('|');
            builder.append("size=");
            builder.append(size());
            builder.append('|');
            builder.append("cacheId=");
            builder.append(cacheId());
            builder.append(')');

            return builder;
        }
    }

    public static int requestIdId()
    {
        return 3;
    }

    public static int requestIdSinceVersion()
    {
        return 0;
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

    public int requestIdLength()
    {
        final int limit = parentMessage.limit();
        return (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
    }

    public int skipRequestId()
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
        final int dataOffset = limit + headerLength;
        parentMessage.limit(dataOffset + dataLength);

        return dataLength;
    }

    public int getRequestId(final MutableDirectBuffer dst, final int dstOffset, final int length)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
        final int bytesCopied = Math.min(length, dataLength);
        parentMessage.limit(limit + headerLength + dataLength);
        buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

        return bytesCopied;
    }

    public int getRequestId(final byte[] dst, final int dstOffset, final int length)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
        final int bytesCopied = Math.min(length, dataLength);
        parentMessage.limit(limit + headerLength + dataLength);
        buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

        return bytesCopied;
    }

    public void wrapRequestId(final DirectBuffer wrapBuffer)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
        parentMessage.limit(limit + headerLength + dataLength);
        wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
    }

    public String requestId()
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF_FFFFL);
        parentMessage.limit(limit + headerLength + dataLength);

        if (0 == dataLength)
        {
            return "";
        }

        final byte[] tmp = new byte[dataLength];
        buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

        final String value;
        try
        {
            value = new String(tmp, "UTF-8");
        }
        catch (final java.io.UnsupportedEncodingException ex)
        {
            throw new RuntimeException(ex);
        }

        return value;
    }

    public String toString()
    {
        if (null == buffer)
        {
            return "";
        }

        final AllCacheStatsResultDecoder decoder = new AllCacheStatsResultDecoder();
        decoder.wrap(buffer, initialOffset, actingBlockLength, actingVersion);

        return decoder.appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        if (null == buffer)
        {
            return builder;
        }

        final int originalLimit = limit();
        limit(initialOffset + actingBlockLength);
        builder.append("[AllCacheStatsResult](sbeTemplateId=");
        builder.append(TEMPLATE_ID);
        builder.append("|sbeSchemaId=");
        builder.append(SCHEMA_ID);
        builder.append("|sbeSchemaVersion=");
        if (parentMessage.actingVersion != SCHEMA_VERSION)
        {
            builder.append(parentMessage.actingVersion);
            builder.append('/');
        }
        builder.append(SCHEMA_VERSION);
        builder.append("|sbeBlockLength=");
        if (actingBlockLength != BLOCK_LENGTH)
        {
            builder.append(actingBlockLength);
            builder.append('/');
        }
        builder.append(BLOCK_LENGTH);
        builder.append("):");
        builder.append("status=");
        builder.append(status());
        builder.append('|');
        builder.append("stats=[");
        StatsDecoder stats = stats();
        if (stats.count() > 0)
        {
            while (stats.hasNext())
            {
                stats.next().appendTo(builder);
                builder.append(',');
            }
            builder.setLength(builder.length() - 1);
        }
        builder.append(']');
        builder.append('|');
        builder.append("requestId=");
        builder.append('\'').append(requestId()).append('\'');

        limit(originalLimit);

        return builder;
    }
}
