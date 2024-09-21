/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;


/**
 * A single sided quote
 */
@SuppressWarnings("all")
public final class SingleSidedQuoteEncoder
{
    public static final int BLOCK_LENGTH = 28;
    public static final int TEMPLATE_ID = 1;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final SingleSidedQuoteEncoder parentMessage = this;
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
        return "SingleSidedQuote";
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

    public SingleSidedQuoteEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public SingleSidedQuoteEncoder wrapAndApplyHeader(
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

    public static int timeId()
    {
        return 1;
    }

    public static int timeSinceVersion()
    {
        return 0;
    }

    public static int timeEncodingOffset()
    {
        return 0;
    }

    public static int timeEncodingLength()
    {
        return 8;
    }

    public static String timeMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static long timeNullValue()
    {
        return 0xffffffffffffffffL;
    }

    public static long timeMinValue()
    {
        return 0x0L;
    }

    public static long timeMaxValue()
    {
        return 0xfffffffffffffffeL;
    }

    public SingleSidedQuoteEncoder time(final long value)
    {
        buffer.putLong(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
        return this;
    }


    public static int symbId()
    {
        return 2;
    }

    public static int symbSinceVersion()
    {
        return 0;
    }

    public static int symbEncodingOffset()
    {
        return 8;
    }

    public static int symbEncodingLength()
    {
        return 1;
    }

    public static String symbMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static short symbNullValue()
    {
        return (short)255;
    }

    public static short symbMinValue()
    {
        return (short)0;
    }

    public static short symbMaxValue()
    {
        return (short)254;
    }

    public SingleSidedQuoteEncoder symb(final short value)
    {
        buffer.putByte(offset + 8, (byte)value);
        return this;
    }


    public static int marketStateId()
    {
        return 3;
    }

    public static int marketStateSinceVersion()
    {
        return 0;
    }

    public static int marketStateEncodingOffset()
    {
        return 9;
    }

    public static int marketStateEncodingLength()
    {
        return 1;
    }

    public static String marketStateMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    public SingleSidedQuoteEncoder marketState(final MarketStateIdentifier value)
    {
        buffer.putByte(offset + 9, (byte)value.value());
        return this;
    }

    public static int quoteConditionId()
    {
        return 4;
    }

    public static int quoteConditionSinceVersion()
    {
        return 0;
    }

    public static int quoteConditionEncodingOffset()
    {
        return 10;
    }

    public static int quoteConditionEncodingLength()
    {
        return 1;
    }

    public static String quoteConditionMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final QuoteConditionEncoder quoteCondition = new QuoteConditionEncoder();

    public QuoteConditionEncoder quoteCondition()
    {
        quoteCondition.wrap(buffer, offset + 10);
        return quoteCondition;
    }

    public static int quoteId()
    {
        return 5;
    }

    public static int quoteSinceVersion()
    {
        return 0;
    }

    public static int quoteEncodingOffset()
    {
        return 11;
    }

    public static int quoteEncodingLength()
    {
        return 17;
    }

    public static String quoteMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    private final SidedQuoteEncoder quote = new SidedQuoteEncoder();

    public SidedQuoteEncoder quote()
    {
        quote.wrap(buffer, offset + 11);
        return quote;
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

        final SingleSidedQuoteDecoder decoder = new SingleSidedQuoteDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
