/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.DirectBuffer;


/**
 * Mass update for the qtys at different prices on the bid side of the book for a single instrument
 */
@SuppressWarnings("all")
public final class MassBidsUpdateDecoder
{
    public static final int BLOCK_LENGTH = 173;
    public static final int TEMPLATE_ID = 3;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MassBidsUpdateDecoder parentMessage = this;
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
        return "MassBidsUpdate";
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

    public MassBidsUpdateDecoder wrap(
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

    public MassBidsUpdateDecoder wrapAndApplyHeader(
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

    public long time()
    {
        return buffer.getLong(offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN);
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

    public short symb()
    {
        return ((short)(buffer.getByte(offset + 8) & 0xFF));
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

    public short marketStateRaw()
    {
        return ((short)(buffer.getByte(offset + 9) & 0xFF));
    }

    public MarketStateIdentifier marketState()
    {
        return MarketStateIdentifier.get(((short)(buffer.getByte(offset + 9) & 0xFF)));
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

    private final QuoteConditionDecoder quoteCondition = new QuoteConditionDecoder();

    public QuoteConditionDecoder quoteCondition()
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

    public short quoteCount()
    {
        return ((short)(buffer.getByte(offset + 11) & 0xFF));
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

    public short quotesRemaining()
    {
        return ((short)(buffer.getByte(offset + 12) & 0xFF));
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

    private final BidQuoteDecoder bidQuote1 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote1()
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

    private final BidQuoteDecoder bidQuote2 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote2()
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

    private final BidQuoteDecoder bidQuote3 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote3()
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

    private final BidQuoteDecoder bidQuote4 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote4()
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

    private final BidQuoteDecoder bidQuote5 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote5()
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

    private final BidQuoteDecoder bidQuote6 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote6()
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

    private final BidQuoteDecoder bidQuote7 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote7()
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

    private final BidQuoteDecoder bidQuote8 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote8()
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

    private final BidQuoteDecoder bidQuote9 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote9()
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

    private final BidQuoteDecoder bidQuote10 = new BidQuoteDecoder();

    public BidQuoteDecoder bidQuote10()
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

        final MassBidsUpdateDecoder decoder = new MassBidsUpdateDecoder();
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
        builder.append("[MassBidsUpdate](sbeTemplateId=");
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
        builder.append("time=");
        builder.append(time());
        builder.append('|');
        builder.append("symb=");
        builder.append(symb());
        builder.append('|');
        builder.append("marketState=");
        builder.append(marketState());
        builder.append('|');
        builder.append("quoteCondition=");
        quoteCondition().appendTo(builder);
        builder.append('|');
        builder.append("quoteCount=");
        builder.append(quoteCount());
        builder.append('|');
        builder.append("quotesRemaining=");
        builder.append(quotesRemaining());
        builder.append('|');
        builder.append("bidQuote1=");
        final BidQuoteDecoder bidQuote1 = bidQuote1();
        if (bidQuote1 != null)
        {
            bidQuote1.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote2=");
        final BidQuoteDecoder bidQuote2 = bidQuote2();
        if (bidQuote2 != null)
        {
            bidQuote2.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote3=");
        final BidQuoteDecoder bidQuote3 = bidQuote3();
        if (bidQuote3 != null)
        {
            bidQuote3.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote4=");
        final BidQuoteDecoder bidQuote4 = bidQuote4();
        if (bidQuote4 != null)
        {
            bidQuote4.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote5=");
        final BidQuoteDecoder bidQuote5 = bidQuote5();
        if (bidQuote5 != null)
        {
            bidQuote5.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote6=");
        final BidQuoteDecoder bidQuote6 = bidQuote6();
        if (bidQuote6 != null)
        {
            bidQuote6.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote7=");
        final BidQuoteDecoder bidQuote7 = bidQuote7();
        if (bidQuote7 != null)
        {
            bidQuote7.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote8=");
        final BidQuoteDecoder bidQuote8 = bidQuote8();
        if (bidQuote8 != null)
        {
            bidQuote8.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote9=");
        final BidQuoteDecoder bidQuote9 = bidQuote9();
        if (bidQuote9 != null)
        {
            bidQuote9.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("bidQuote10=");
        final BidQuoteDecoder bidQuote10 = bidQuote10();
        if (bidQuote10 != null)
        {
            bidQuote10.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }

        limit(originalLimit);

        return builder;
    }
}
