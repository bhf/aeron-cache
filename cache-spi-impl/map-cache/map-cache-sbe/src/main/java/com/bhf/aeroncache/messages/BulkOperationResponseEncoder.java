/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

import org.agrona.MutableDirectBuffer;
import org.agrona.DirectBuffer;


/**
 * Response from a bulk operation request
 */
@SuppressWarnings("all")
public final class BulkOperationResponseEncoder
{
    public static final int BLOCK_LENGTH = 0;
    public static final int TEMPLATE_ID = 23;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final String SEMANTIC_VERSION = "0.1";
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final BulkOperationResponseEncoder parentMessage = this;
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
        return "BulkOpsResponse";
    }

    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public BulkOperationResponseEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;
        limit(offset + BLOCK_LENGTH);

        return this;
    }

    public BulkOperationResponseEncoder wrapAndApplyHeader(
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
        private final BulkOperationResponseEncoder parentMessage;
        private MutableDirectBuffer buffer;
        private int count;
        private int index;
        private int offset;
        private int initialLimit;

        ItemsEncoder(final BulkOperationResponseEncoder parentMessage)
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
            buffer.putShort(limit + 0, (short)1, BYTE_ORDER);
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
            return 1;
        }

        public static int operationStatusId()
        {
            return 30;
        }

        public static int operationStatusSinceVersion()
        {
            return 0;
        }

        public static int operationStatusEncodingOffset()
        {
            return 0;
        }

        public static int operationStatusEncodingLength()
        {
            return 1;
        }

        public static String operationStatusMetaAttribute(final MetaAttribute metaAttribute)
        {
            if (MetaAttribute.PRESENCE == metaAttribute)
            {
                return "required";
            }

            return "";
        }

        public ItemsEncoder operationStatus(final OperationStatus value)
        {
            buffer.putByte(offset + 0, (byte)value.value());
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

    public BulkOperationResponseEncoder putRequestId(final DirectBuffer src, final int srcOffset, final int length)
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

    public BulkOperationResponseEncoder putRequestId(final byte[] src, final int srcOffset, final int length)
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

    public BulkOperationResponseEncoder requestId(final String value)
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

        final BulkOperationResponseDecoder decoder = new BulkOperationResponseDecoder();
        decoder.wrap(buffer, offset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
