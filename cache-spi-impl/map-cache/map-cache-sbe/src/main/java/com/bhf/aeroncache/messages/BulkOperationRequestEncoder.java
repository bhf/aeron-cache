/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;


/**
 * Conduct a bulk operation across the entire cache
 */
@SuppressWarnings("all")
public final class BulkOperationRequestEncoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 22;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final String SEMANTIC_VERSION = "0.1";
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final BulkOperationRequestEncoder parentMessage = this;
    private MutableDirectBuffer buffer;
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
        return "BulkOpsRequest";
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public BulkOperationRequestEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public BulkOperationRequestEncoder wrapAndApplyHeader(
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

    private final ItemsEncoder items = new ItemsEncoder(this);

    public static long itemsId()
    {
        return 30;
    }

    public ItemsEncoder itemsCount(final int count)
    {
        items.wrap(buffer, count);
        return items;
    }

    public static final class ItemsEncoder
    {
        public static final int HEADER_SIZE = 4;
        private final BulkOperationRequestEncoder parentMessage;
        private MutableDirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int initialLimit;

        ItemsEncoder(final BulkOperationRequestEncoder parentMessage)
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
            buffer.putShort(limit + 0, (short)9, BYTE_ORDER);
            buffer.putShort(limit + 2, (short)count, BYTE_ORDER);
        }

        public ItemsEncoder next()
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
            buffer.putShort(initialLimit + 2, (short)count, BYTE_ORDER);

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
            return 9;
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

        public ItemsEncoder operationType(final BulkOperationType value)
        {
            buffer.putByte(offset + 0, (byte)value.value());
            return this;
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

        public ItemsEncoder ttl(final long value)
        {
            buffer.putLong(offset + 1, value, BYTE_ORDER);
            return this;
        }


        public static int requestIdId()
        {
            return 31;
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

        public ItemsEncoder putRequestId(final DirectBuffer src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder putRequestId(final byte[] src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder requestId(final String value)
        {
            final byte[] bytes = (null == value || value.isEmpty()) ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final int length = bytes.length;
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, bytes, 0, length);

            return this;
        }

        public static int cacheIdId()
        {
            return 32;
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

        public ItemsEncoder putCacheId(final DirectBuffer src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder putCacheId(final byte[] src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder cacheId(final String value)
        {
            final byte[] bytes = (null == value || value.isEmpty()) ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final int length = bytes.length;
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, bytes, 0, length);

            return this;
        }

        public static int keyId()
        {
            return 33;
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

        public ItemsEncoder putKey(final DirectBuffer src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder putKey(final byte[] src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder key(final String value)
        {
            final byte[] bytes = (null == value || value.isEmpty()) ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final int length = bytes.length;
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, bytes, 0, length);

            return this;
        }

        public static int valueId()
        {
            return 34;
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

        public ItemsEncoder putValue(final DirectBuffer src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder putValue(final byte[] src, final int srcOffset, final int length)
        {
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, src, srcOffset, length);

            return this;
        }

        public ItemsEncoder value(final String value)
        {
            final byte[] bytes = (null == value || value.isEmpty()) ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            final int length = bytes.length;
            if (length > 1073741824)
            {
                throw new IllegalStateException("length > maxValue for type: " + length);
            }

            final int headerLength = 4;
            final int limit = parentMessage.limit();
            parentMessage.limit(limit + headerLength + length);
            buffer.putInt(limit, length, BYTE_ORDER);
            buffer.putBytes(limit + headerLength, bytes, 0, length);

            return this;
        }
    }

    public static int requestIdId()
    {
        return 4;
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

    public BulkOperationRequestEncoder putRequestId(final DirectBuffer src, final int srcOffset, final int length)
    {
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, BYTE_ORDER);
        buffer.putBytes(limit + headerLength, src, srcOffset, length);

        return this;
    }

    public BulkOperationRequestEncoder putRequestId(final byte[] src, final int srcOffset, final int length)
    {
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, BYTE_ORDER);
        buffer.putBytes(limit + headerLength, src, srcOffset, length);

        return this;
    }

    public BulkOperationRequestEncoder requestId(final String value)
    {
        final byte[] bytes = (null == value || value.isEmpty()) ? org.agrona.collections.ArrayUtil.EMPTY_BYTE_ARRAY : value.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        final int length = bytes.length;
        if (length > 1073741824)
        {
            throw new IllegalStateException("length > maxValue for type: " + length);
        }

        final int headerLength = 4;
        final int limit = parentMessage.limit();
        parentMessage.limit(limit + headerLength + length);
        buffer.putInt(limit, length, BYTE_ORDER);
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

        final BulkOperationRequestDecoder decoder = new BulkOperationRequestDecoder();
        decoder.wrap(buffer, offset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
