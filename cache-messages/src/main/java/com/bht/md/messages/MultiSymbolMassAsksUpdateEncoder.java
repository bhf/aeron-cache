/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;


/**
 * MultiSymbolMass update for the qtys at different prices on the ask side of the book for multiple instrument
 */
@SuppressWarnings("all")
public final class MultiSymbolMassAsksUpdateEncoder
{
    public static final int BLOCK_LENGTH = 182;
    public static final int TEMPLATE_ID = 8;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MultiSymbolMassAsksUpdateEncoder parentMessage = this;
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
        return "MultiSymbolMassAsksUpdate";
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

    public MultiSymbolMassAsksUpdateEncoder wrap(final MutableDirectBuffer buffer, final int offset)
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

    public MultiSymbolMassAsksUpdateEncoder wrapAndApplyHeader(
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

    public MultiSymbolMassAsksUpdateEncoder time(final long value)
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

    public MultiSymbolMassAsksUpdateEncoder marketState(final MarketStateIdentifier value)
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

    public MultiSymbolMassAsksUpdateEncoder quoteCount(final short value)
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

    public MultiSymbolMassAsksUpdateEncoder quotesRemaining(final short value)
    {
        buffer.putByte(offset + 11, (byte)value);
        return this;
    }


    public static int askQuote1Id()
    {
        return 7;
    }

    public static int askQuote1SinceVersion()
    {
        return 0;
    }

    public static int askQuote1EncodingOffset()
    {
        return 12;
    }

    public static int askQuote1EncodingLength()
    {
        return 17;
    }

    public static String askQuote1MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote1 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote1()
    {
        askQuote1.wrap(buffer, offset + 12);
        return askQuote1;
    }

    public static int askQuote2Id()
    {
        return 8;
    }

    public static int askQuote2SinceVersion()
    {
        return 0;
    }

    public static int askQuote2EncodingOffset()
    {
        return 29;
    }

    public static int askQuote2EncodingLength()
    {
        return 17;
    }

    public static String askQuote2MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote2 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote2()
    {
        askQuote2.wrap(buffer, offset + 29);
        return askQuote2;
    }

    public static int askQuote3Id()
    {
        return 9;
    }

    public static int askQuote3SinceVersion()
    {
        return 0;
    }

    public static int askQuote3EncodingOffset()
    {
        return 46;
    }

    public static int askQuote3EncodingLength()
    {
        return 17;
    }

    public static String askQuote3MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote3 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote3()
    {
        askQuote3.wrap(buffer, offset + 46);
        return askQuote3;
    }

    public static int askQuote4Id()
    {
        return 10;
    }

    public static int askQuote4SinceVersion()
    {
        return 0;
    }

    public static int askQuote4EncodingOffset()
    {
        return 63;
    }

    public static int askQuote4EncodingLength()
    {
        return 17;
    }

    public static String askQuote4MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote4 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote4()
    {
        askQuote4.wrap(buffer, offset + 63);
        return askQuote4;
    }

    public static int askQuote5Id()
    {
        return 11;
    }

    public static int askQuote5SinceVersion()
    {
        return 0;
    }

    public static int askQuote5EncodingOffset()
    {
        return 80;
    }

    public static int askQuote5EncodingLength()
    {
        return 17;
    }

    public static String askQuote5MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote5 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote5()
    {
        askQuote5.wrap(buffer, offset + 80);
        return askQuote5;
    }

    public static int askQuote6Id()
    {
        return 12;
    }

    public static int askQuote6SinceVersion()
    {
        return 0;
    }

    public static int askQuote6EncodingOffset()
    {
        return 97;
    }

    public static int askQuote6EncodingLength()
    {
        return 17;
    }

    public static String askQuote6MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote6 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote6()
    {
        askQuote6.wrap(buffer, offset + 97);
        return askQuote6;
    }

    public static int askQuote7Id()
    {
        return 13;
    }

    public static int askQuote7SinceVersion()
    {
        return 0;
    }

    public static int askQuote7EncodingOffset()
    {
        return 114;
    }

    public static int askQuote7EncodingLength()
    {
        return 17;
    }

    public static String askQuote7MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote7 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote7()
    {
        askQuote7.wrap(buffer, offset + 114);
        return askQuote7;
    }

    public static int askQuote8Id()
    {
        return 14;
    }

    public static int askQuote8SinceVersion()
    {
        return 0;
    }

    public static int askQuote8EncodingOffset()
    {
        return 131;
    }

    public static int askQuote8EncodingLength()
    {
        return 17;
    }

    public static String askQuote8MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote8 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote8()
    {
        askQuote8.wrap(buffer, offset + 131);
        return askQuote8;
    }

    public static int askQuote9Id()
    {
        return 15;
    }

    public static int askQuote9SinceVersion()
    {
        return 0;
    }

    public static int askQuote9EncodingOffset()
    {
        return 148;
    }

    public static int askQuote9EncodingLength()
    {
        return 17;
    }

    public static String askQuote9MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote9 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote9()
    {
        askQuote9.wrap(buffer, offset + 148);
        return askQuote9;
    }

    public static int askQuote10Id()
    {
        return 16;
    }

    public static int askQuote10SinceVersion()
    {
        return 0;
    }

    public static int askQuote10EncodingOffset()
    {
        return 165;
    }

    public static int askQuote10EncodingLength()
    {
        return 17;
    }

    public static String askQuote10MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final SymbolAskQuoteEncoder askQuote10 = new SymbolAskQuoteEncoder();

    public SymbolAskQuoteEncoder askQuote10()
    {
        askQuote10.wrap(buffer, offset + 165);
        return askQuote10;
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

        final MultiSymbolMassAsksUpdateDecoder decoder = new MultiSymbolMassAsksUpdateDecoder();
        decoder.wrap(buffer, initialOffset, BLOCK_LENGTH, SCHEMA_VERSION);

        return decoder.appendTo(builder);
    }
}
