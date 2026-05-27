/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;


/**
 * Conduct a bulk operation across the entire cache
 */
@SuppressWarnings("all")
public final class BulkOperationRequestDecoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 22;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final String SEMANTIC_VERSION = "0.1";
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final BulkOperationRequestDecoder parentMessage = this;
    private DirectBuffer buffer;
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
        return "BulkOpsRequest";
    }

    public DirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public BulkOperationRequestDecoder wrap(
        final DirectBuffer buffer,
        final int offset,
        final int actingBlockLength,
        final int actingVersion)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        this.actingBlockLength = actingBlockLength;
        this.actingVersion = actingVersion;
        limit(offset + actingBlockLength);

        return this;
    }

    public BulkOperationRequestDecoder wrapAndApplyHeader(
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

    public BulkOperationRequestDecoder sbeRewind()
    {
        return wrap(buffer, offset, actingBlockLength, actingVersion);
    }

    public int sbeDecodedLength()
    {
        final int currentLimit = limit();
        sbeSkip();
        final int decodedLength = encodedLength();
        limit(currentLimit);

        return decodedLength;
    }

    public int actingVersion()
    {
        return actingVersion;
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

    private final ItemsDecoder items = new ItemsDecoder(this);

    public static long itemsDecoderId()
    {
        return 30;
    }

    public static int itemsDecoderSinceVersion()
    {
        return 0;
    }

    public ItemsDecoder items()
    {
        items.wrap(buffer);
        return items;
    }

    public static final class ItemsDecoder
        implements Iterable<ItemsDecoder>, java.util.Iterator<ItemsDecoder>
    {
        public static final int HEADER_SIZE = 4;
        private final BulkOperationRequestDecoder parentMessage;
        private DirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int blockLength;

        ItemsDecoder(final BulkOperationRequestDecoder parentMessage)
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
            blockLength = (buffer.getShort(limit + 0, BYTE_ORDER) & 0xFFFF);
            count = (buffer.getShort(limit + 2, BYTE_ORDER) & 0xFFFF);
        }

        public ItemsDecoder next()
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
            return 9;
        }

        public int actingBlockLength()
        {
            return blockLength;
        }

        public int actingVersion()
        {
            return parentMessage.actingVersion;
        }

        public int count()
        {
            return count;
        }

        public java.util.Iterator<ItemsDecoder> iterator()
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

        public static int operationTypeId()
        {
            return 29;
        }

        public static int operationTypeSinceVersion()
        {
            return 0;
        }

        public static int operationTypeEncodingOffset()
        {
            return 0;
        }

        public static int operationTypeEncodingLength()
        {
            return 1;
        }

        public static String operationTypeMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public short operationTypeRaw()
        {
            return ((short)(buffer.getByte(offset + 0) & 0xFF));
        }

        public BulkOperationType operationType()
        {
            return BulkOperationType.get(((short)(buffer.getByte(offset + 0) & 0xFF)));
        }


        public static int ttlId()
        {
            return 30;
        }

        public static int ttlSinceVersion()
        {
            return 0;
        }

        public static int ttlEncodingOffset()
        {
            return 1;
        }

        public static int ttlEncodingLength()
        {
            return 8;
        }

        public static String ttlMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static long ttlNullValue()
        {
            return -9223372036854775808L;
        }

        public static long ttlMinValue()
        {
            return -9223372036854775807L;
        }

        public static long ttlMaxValue()
        {
            return 9223372036854775807L;
        }

        public long ttl()
        {
            return buffer.getLong(offset + 1, BYTE_ORDER);
        }


        public static int requestIdId()
        {
            return 31;
        }

        public static int requestIdSinceVersion()
        {
            return 0;
        }

        public static String requestIdCharacterEncoding()
        {
            return java.nio.charset.StandardCharsets.UTF_8.name();
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
            return (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        }

        public int skipRequestId()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int dataOffset = limit + headerLength;
            parentMessage.limit(dataOffset + dataLength);

            return dataLength;
        }

        public int getRequestId(final MutableDirectBuffer dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public int getRequestId(final byte[] dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public void wrapRequestId(final DirectBuffer wrapBuffer)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);
            wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
        }

        public String requestId()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);

            if (0 == dataLength)
            {
                return "";
            }

            final byte[] tmp = new byte[dataLength];
            buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

            return new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        }

        public static int cacheIdId()
        {
            return 32;
        }

        public static int cacheIdSinceVersion()
        {
            return 0;
        }

        public static String cacheIdCharacterEncoding()
        {
            return java.nio.charset.StandardCharsets.UTF_8.name();
        }

        public static String cacheIdMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static int cacheIdHeaderLength()
        {
            return 4;
        }

        public int cacheIdLength()
        {
            final int limit = parentMessage.limit();
            return (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        }

        public int skipCacheId()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int dataOffset = limit + headerLength;
            parentMessage.limit(dataOffset + dataLength);

            return dataLength;
        }

        public int getCacheId(final MutableDirectBuffer dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public int getCacheId(final byte[] dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public void wrapCacheId(final DirectBuffer wrapBuffer)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);
            wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
        }

        public String cacheId()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);

            if (0 == dataLength)
            {
                return "";
            }

            final byte[] tmp = new byte[dataLength];
            buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

            return new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        }

        public static int keyId()
        {
            return 33;
        }

        public static int keySinceVersion()
        {
            return 0;
        }

        public static String keyCharacterEncoding()
        {
            return java.nio.charset.StandardCharsets.UTF_8.name();
        }

        public static String keyMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static int keyHeaderLength()
        {
            return 4;
        }

        public int keyLength()
        {
            final int limit = parentMessage.limit();
            return (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        }

        public int skipKey()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int dataOffset = limit + headerLength;
            parentMessage.limit(dataOffset + dataLength);

            return dataLength;
        }

        public int getKey(final MutableDirectBuffer dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public int getKey(final byte[] dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public void wrapKey(final DirectBuffer wrapBuffer)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);
            wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
        }

        public String key()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);

            if (0 == dataLength)
            {
                return "";
            }

            final byte[] tmp = new byte[dataLength];
            buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

            return new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        }

        public static int valueId()
        {
            return 34;
        }

        public static int valueSinceVersion()
        {
            return 0;
        }

        public static String valueCharacterEncoding()
        {
            return java.nio.charset.StandardCharsets.UTF_8.name();
        }

        public static String valueMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public static int valueHeaderLength()
        {
            return 4;
        }

        public int valueLength()
        {
            final int limit = parentMessage.limit();
            return (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        }

        public int skipValue()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int dataOffset = limit + headerLength;
            parentMessage.limit(dataOffset + dataLength);

            return dataLength;
        }

        public int getValue(final MutableDirectBuffer dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public int getValue(final byte[] dst, final int dstOffset, final int length)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            final int bytesCopied = Math.min(length, dataLength);
            parentMessage.limit(limit + headerLength + dataLength);
            buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

            return bytesCopied;
        }

        public void wrapValue(final DirectBuffer wrapBuffer)
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);
            wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
        }

        public String value()
        {
            final int headerLength = 4;
            final int limit = parentMessage.limit();
            final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
            parentMessage.limit(limit + headerLength + dataLength);

            if (0 == dataLength)
            {
                return "";
            }

            final byte[] tmp = new byte[dataLength];
            buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

            return new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        }

        public StringBuilder appendTo(final StringBuilder builder)
        {
            if (null == buffer)
            {
                return builder;
            }

            builder.append('(');
            builder.append("operationType=");
            builder.append(this.operationType());
            builder.append('|');
            builder.append("ttl=");
            builder.append(this.ttl());
            builder.append('|');
            builder.append("requestId=");
            builder.append('\'').append(requestId()).append('\'');
            builder.append('|');
            builder.append("cacheId=");
            builder.append('\'').append(cacheId()).append('\'');
            builder.append('|');
            builder.append("key=");
            builder.append('\'').append(key()).append('\'');
            builder.append('|');
            builder.append("value=");
            builder.append('\'').append(value()).append('\'');
            builder.append(')');

            return builder;
        }
        
        public ItemsDecoder sbeSkip()
        {
            skipRequestId();
            skipCacheId();
            skipKey();
            skipValue();

            return this;
        }
    }

    public static int requestIdId()
    {
        return 4;
    }

    public static int requestIdSinceVersion()
    {
        return 0;
    }

    public static String requestIdCharacterEncoding()
    {
        return java.nio.charset.StandardCharsets.UTF_8.name();
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
        return (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
    }

    public int skipRequestId()
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        final int dataOffset = limit + headerLength;
        parentMessage.limit(dataOffset + dataLength);

        return dataLength;
    }

    public int getRequestId(final MutableDirectBuffer dst, final int dstOffset, final int length)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        final int bytesCopied = Math.min(length, dataLength);
        parentMessage.limit(limit + headerLength + dataLength);
        buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

        return bytesCopied;
    }

    public int getRequestId(final byte[] dst, final int dstOffset, final int length)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        final int bytesCopied = Math.min(length, dataLength);
        parentMessage.limit(limit + headerLength + dataLength);
        buffer.getBytes(limit + headerLength, dst, dstOffset, bytesCopied);

        return bytesCopied;
    }

    public void wrapRequestId(final DirectBuffer wrapBuffer)
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        parentMessage.limit(limit + headerLength + dataLength);
        wrapBuffer.wrap(buffer, limit + headerLength, dataLength);
    }

    public String requestId()
    {
        final int headerLength = 4;
        final int limit = parentMessage.limit();
        final int dataLength = (int)(buffer.getInt(limit, BYTE_ORDER) & 0xFFFF_FFFFL);
        parentMessage.limit(limit + headerLength + dataLength);

        if (0 == dataLength)
        {
            return "";
        }

        final byte[] tmp = new byte[dataLength];
        buffer.getBytes(limit + headerLength, tmp, 0, dataLength);

        return new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String toString()
    {
        if (null == buffer)
        {
            return "";
        }

        final BulkOperationRequestDecoder decoder = new BulkOperationRequestDecoder();
        decoder.wrap(buffer, offset, actingBlockLength, actingVersion);

        return decoder.appendTo(new StringBuilder()).toString();
    }

    public StringBuilder appendTo(final StringBuilder builder)
    {
        if (null == buffer)
        {
            return builder;
        }

        final int originalLimit = limit();
        limit(offset + actingBlockLength);
        builder.append("[BulkOperationRequest](sbeTemplateId=");
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
        builder.append("items=[");
        final int itemsOriginalOffset = items.offset;
        final int itemsOriginalIndex = items.index;
        final ItemsDecoder items = this.items();
        if (items.count() > 0)
        {
            while (items.hasNext())
            {
                items.next().appendTo(builder);
                builder.append(',');
            }
            builder.setLength(builder.length() - 1);
        }
        items.offset = itemsOriginalOffset;
        items.index = itemsOriginalIndex;
        builder.append(']');
        builder.append('|');
        builder.append("requestId=");
        builder.append('\'').append(requestId()).append('\'');

        limit(originalLimit);

        return builder;
    }
    
    public BulkOperationRequestDecoder sbeSkip()
    {
        sbeRewind();
        ItemsDecoder items = this.items();
        if (items.count() > 0)
        {
            while (items.hasNext())
            {
                items.next();
                items.sbeSkip();
            }
        }
        skipRequestId();

        return this;
    }
}
