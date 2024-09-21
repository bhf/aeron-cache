/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;


/**
 * MultiSymbolMass update for the qtys at different prices on either side of the                  book for multiple instrument
 */
@SuppressWarnings("all")
public final class MultiSymbolMassSidedUpdateEncoder
{
    public static final int BLOCK_LENGTH = 192;
    public static final int TEMPLATE_ID = 9;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MultiSymbolMassSidedUpdateEncoder parentMessage = this;
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
        return "MultiSymbolMassSidedUpdate";
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

    public MultiSymbolMassSidedUpdateEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public MultiSymbolMassSidedUpdateEncoder wrapAndApplyHeader(
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

    public MultiSymbolMassSidedUpdateEncoder time(final long value)
    {
        buffer.putLong(offset + 0, value, java.nio.ByteOrder.LITTLE_ENDIAN);
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
        return 8;
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

    public MultiSymbolMassSidedUpdateEncoder marketState(final MarketStateIdentifier value)
    {
        buffer.putByte(offset + 8, (byte)value.value());
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
        return 9;
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
        quoteCondition.wrap(buffer, offset + 9);
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
        return 10;
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

    public MultiSymbolMassSidedUpdateEncoder quoteCount(final short value)
    {
        buffer.putByte(offset + 10, (byte)value);
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
        return 11;
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

    public MultiSymbolMassSidedUpdateEncoder quotesRemaining(final short value)
    {
        buffer.putByte(offset + 11, (byte)value);
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
        return 12;
    }

    public static int sidedQuote1EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote1MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote1 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote1()
    {
        sidedQuote1.wrap(buffer, offset + 12);
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
        return 18;
    }

    public static String sidedQuote2MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote2 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote2()
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
        return 48;
    }

    public static int sidedQuote3EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote3MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote3 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote3()
    {
        sidedQuote3.wrap(buffer, offset + 48);
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
        return 66;
    }

    public static int sidedQuote4EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote4MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote4 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote4()
    {
        sidedQuote4.wrap(buffer, offset + 66);
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
        return 84;
    }

    public static int sidedQuote5EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote5MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote5 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote5()
    {
        sidedQuote5.wrap(buffer, offset + 84);
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
        return 102;
    }

    public static int sidedQuote6EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote6MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote6 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote6()
    {
        sidedQuote6.wrap(buffer, offset + 102);
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
        return 120;
    }

    public static int sidedQuote7EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote7MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote7 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote7()
    {
        sidedQuote7.wrap(buffer, offset + 120);
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
        return 138;
    }

    public static int sidedQuote8EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote8MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote8 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote8()
    {
        sidedQuote8.wrap(buffer, offset + 138);
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
        return 156;
    }

    public static int sidedQuote9EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote9MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote9 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote9()
    {
        sidedQuote9.wrap(buffer, offset + 156);
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
        return 174;
    }

    public static int sidedQuote10EncodingLength()
    {
        return 18;
    }

    public static String sidedQuote10MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolSidedQuoteEncoder sidedQuote10 = new SymbolSidedQuoteEncoder();

    public SymbolSidedQuoteEncoder sidedQuote10()
    {
        sidedQuote10.wrap(buffer, offset + 174);
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

        final MultiSymbolMassSidedUpdateDecoder decoder = new MultiSymbolMassSidedUpdateDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
