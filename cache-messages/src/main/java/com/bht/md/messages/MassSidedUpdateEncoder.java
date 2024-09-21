/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;


/**
 * Mass update for the qtys at different prices on either side of the book for a single instrument
 */
@SuppressWarnings("all")
public final class MassSidedUpdateEncoder
{
    public static final int BLOCK_LENGTH = 183;
    public static final int TEMPLATE_ID = 5;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MassSidedUpdateEncoder parentMessage = this;
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
        return "MassSidedUpdate";
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

    public MassSidedUpdateEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public MassSidedUpdateEncoder wrapAndApplyHeader(
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

    public MassSidedUpdateEncoder time(final long value)
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

    public MassSidedUpdateEncoder symb(final short value)
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

    public MassSidedUpdateEncoder marketState(final MarketStateIdentifier value)
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

    public static int quoteCountId()
    {
        return 5;
    }

    public static int quoteCountSinceVersion()
    {
        return 0;
    }

    public static int quoteCountEncodingOffset()
    {
        return 11;
    }

    public static int quoteCountEncodingLength()
    {
        return 1;
    }

    public static String quoteCountMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static short quoteCountNullValue()
    {
        return (short)255;
    }

    public static short quoteCountMinValue()
    {
        return (short)0;
    }

    public static short quoteCountMaxValue()
    {
        return (short)254;
    }

    public MassSidedUpdateEncoder quoteCount(final short value)
    {
        buffer.putByte(offset + 11, (byte)value);
        return this;
    }


    public static int quotesRemainingId()
    {
        return 6;
    }

    public static int quotesRemainingSinceVersion()
    {
        return 0;
    }

    public static int quotesRemainingEncodingOffset()
    {
        return 12;
    }

    public static int quotesRemainingEncodingLength()
    {
        return 1;
    }

    public static String quotesRemainingMetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "required";
        }

        return "";
    }

    public static short quotesRemainingNullValue()
    {
        return (short)255;
    }

    public static short quotesRemainingMinValue()
    {
        return (short)0;
    }

    public static short quotesRemainingMaxValue()
    {
        return (short)254;
    }

    public MassSidedUpdateEncoder quotesRemaining(final short value)
    {
        buffer.putByte(offset + 12, (byte)value);
        return this;
    }


    public static int sidedQuote1Id()
    {
        return 7;
    }

    public static int sidedQuote1SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote1EncodingOffset()
    {
        return 13;
    }

    public static int sidedQuote1EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote1MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote1 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote1()
    {
        sidedQuote1.wrap(buffer, offset + 13);
        return sidedQuote1;
    }

    public static int sidedQuote2Id()
    {
        return 8;
    }

    public static int sidedQuote2SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote2EncodingOffset()
    {
        return 30;
    }

    public static int sidedQuote2EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote2MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote2 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote2()
    {
        sidedQuote2.wrap(buffer, offset + 30);
        return sidedQuote2;
    }

    public static int sidedQuote3Id()
    {
        return 9;
    }

    public static int sidedQuote3SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote3EncodingOffset()
    {
        return 47;
    }

    public static int sidedQuote3EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote3MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote3 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote3()
    {
        sidedQuote3.wrap(buffer, offset + 47);
        return sidedQuote3;
    }

    public static int sidedQuote4Id()
    {
        return 10;
    }

    public static int sidedQuote4SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote4EncodingOffset()
    {
        return 64;
    }

    public static int sidedQuote4EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote4MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote4 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote4()
    {
        sidedQuote4.wrap(buffer, offset + 64);
        return sidedQuote4;
    }

    public static int sidedQuote5Id()
    {
        return 11;
    }

    public static int sidedQuote5SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote5EncodingOffset()
    {
        return 81;
    }

    public static int sidedQuote5EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote5MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote5 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote5()
    {
        sidedQuote5.wrap(buffer, offset + 81);
        return sidedQuote5;
    }

    public static int sidedQuote6Id()
    {
        return 12;
    }

    public static int sidedQuote6SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote6EncodingOffset()
    {
        return 98;
    }

    public static int sidedQuote6EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote6MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote6 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote6()
    {
        sidedQuote6.wrap(buffer, offset + 98);
        return sidedQuote6;
    }

    public static int sidedQuote7Id()
    {
        return 13;
    }

    public static int sidedQuote7SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote7EncodingOffset()
    {
        return 115;
    }

    public static int sidedQuote7EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote7MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote7 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote7()
    {
        sidedQuote7.wrap(buffer, offset + 115);
        return sidedQuote7;
    }

    public static int sidedQuote8Id()
    {
        return 14;
    }

    public static int sidedQuote8SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote8EncodingOffset()
    {
        return 132;
    }

    public static int sidedQuote8EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote8MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote8 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote8()
    {
        sidedQuote8.wrap(buffer, offset + 132);
        return sidedQuote8;
    }

    public static int sidedQuote9Id()
    {
        return 15;
    }

    public static int sidedQuote9SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote9EncodingOffset()
    {
        return 149;
    }

    public static int sidedQuote9EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote9MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote9 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote9()
    {
        sidedQuote9.wrap(buffer, offset + 149);
        return sidedQuote9;
    }

    public static int sidedQuote10Id()
    {
        return 16;
    }

    public static int sidedQuote10SinceVersion()
    {
        return 0;
    }

    public static int sidedQuote10EncodingOffset()
    {
        return 166;
    }

    public static int sidedQuote10EncodingLength()
    {
        return 17;
    }

    public static String sidedQuote10MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SidedQuoteEncoder sidedQuote10 = new SidedQuoteEncoder();

    public SidedQuoteEncoder sidedQuote10()
    {
        sidedQuote10.wrap(buffer, offset + 166);
        return sidedQuote10;
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

        final MassSidedUpdateDecoder decoder = new MassSidedUpdateDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
