/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;


/**
 * Mass update for the qtys at different prices on the bid side of the book for a single instrument
 */
@SuppressWarnings("all")
public final class MassBidsUpdateEncoder
{
    public static final int BLOCK_LENGTH = 173;
    public static final int TEMPLATE_ID = 3;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MassBidsUpdateEncoder parentMessage = this;
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
        return "MassBidsUpdate";
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

    public MassBidsUpdateEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public MassBidsUpdateEncoder wrapAndApplyHeader(
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

    public MassBidsUpdateEncoder time(final long value)
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

    public MassBidsUpdateEncoder symb(final short value)
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

    public MassBidsUpdateEncoder marketState(final MarketStateIdentifier value)
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

    public MassBidsUpdateEncoder quoteCount(final short value)
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

    public MassBidsUpdateEncoder quotesRemaining(final short value)
    {
        buffer.putByte(offset + 12, (byte)value);
        return this;
    }


    public static int bidQuote1Id()
    {
        return 7;
    }

    public static int bidQuote1SinceVersion()
    {
        return 0;
    }

    public static int bidQuote1EncodingOffset()
    {
        return 13;
    }

    public static int bidQuote1EncodingLength()
    {
        return 16;
    }

    public static String bidQuote1MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote1 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote1()
    {
        bidQuote1.wrap(buffer, offset + 13);
        return bidQuote1;
    }

    public static int bidQuote2Id()
    {
        return 8;
    }

    public static int bidQuote2SinceVersion()
    {
        return 0;
    }

    public static int bidQuote2EncodingOffset()
    {
        return 29;
    }

    public static int bidQuote2EncodingLength()
    {
        return 16;
    }

    public static String bidQuote2MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote2 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote2()
    {
        bidQuote2.wrap(buffer, offset + 29);
        return bidQuote2;
    }

    public static int bidQuote3Id()
    {
        return 9;
    }

    public static int bidQuote3SinceVersion()
    {
        return 0;
    }

    public static int bidQuote3EncodingOffset()
    {
        return 45;
    }

    public static int bidQuote3EncodingLength()
    {
        return 16;
    }

    public static String bidQuote3MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote3 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote3()
    {
        bidQuote3.wrap(buffer, offset + 45);
        return bidQuote3;
    }

    public static int bidQuote4Id()
    {
        return 10;
    }

    public static int bidQuote4SinceVersion()
    {
        return 0;
    }

    public static int bidQuote4EncodingOffset()
    {
        return 61;
    }

    public static int bidQuote4EncodingLength()
    {
        return 16;
    }

    public static String bidQuote4MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote4 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote4()
    {
        bidQuote4.wrap(buffer, offset + 61);
        return bidQuote4;
    }

    public static int bidQuote5Id()
    {
        return 11;
    }

    public static int bidQuote5SinceVersion()
    {
        return 0;
    }

    public static int bidQuote5EncodingOffset()
    {
        return 77;
    }

    public static int bidQuote5EncodingLength()
    {
        return 16;
    }

    public static String bidQuote5MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote5 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote5()
    {
        bidQuote5.wrap(buffer, offset + 77);
        return bidQuote5;
    }

    public static int bidQuote6Id()
    {
        return 12;
    }

    public static int bidQuote6SinceVersion()
    {
        return 0;
    }

    public static int bidQuote6EncodingOffset()
    {
        return 93;
    }

    public static int bidQuote6EncodingLength()
    {
        return 16;
    }

    public static String bidQuote6MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote6 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote6()
    {
        bidQuote6.wrap(buffer, offset + 93);
        return bidQuote6;
    }

    public static int bidQuote7Id()
    {
        return 13;
    }

    public static int bidQuote7SinceVersion()
    {
        return 0;
    }

    public static int bidQuote7EncodingOffset()
    {
        return 109;
    }

    public static int bidQuote7EncodingLength()
    {
        return 16;
    }

    public static String bidQuote7MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote7 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote7()
    {
        bidQuote7.wrap(buffer, offset + 109);
        return bidQuote7;
    }

    public static int bidQuote8Id()
    {
        return 14;
    }

    public static int bidQuote8SinceVersion()
    {
        return 0;
    }

    public static int bidQuote8EncodingOffset()
    {
        return 125;
    }

    public static int bidQuote8EncodingLength()
    {
        return 16;
    }

    public static String bidQuote8MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote8 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote8()
    {
        bidQuote8.wrap(buffer, offset + 125);
        return bidQuote8;
    }

    public static int bidQuote9Id()
    {
        return 15;
    }

    public static int bidQuote9SinceVersion()
    {
        return 0;
    }

    public static int bidQuote9EncodingOffset()
    {
        return 141;
    }

    public static int bidQuote9EncodingLength()
    {
        return 16;
    }

    public static String bidQuote9MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote9 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote9()
    {
        bidQuote9.wrap(buffer, offset + 141);
        return bidQuote9;
    }

    public static int bidQuote10Id()
    {
        return 16;
    }

    public static int bidQuote10SinceVersion()
    {
        return 0;
    }

    public static int bidQuote10EncodingOffset()
    {
        return 157;
    }

    public static int bidQuote10EncodingLength()
    {
        return 16;
    }

    public static String bidQuote10MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final BidQuoteEncoder bidQuote10 = new BidQuoteEncoder();

    public BidQuoteEncoder bidQuote10()
    {
        bidQuote10.wrap(buffer, offset + 157);
        return bidQuote10;
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

        final MassBidsUpdateDecoder decoder = new MassBidsUpdateDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
