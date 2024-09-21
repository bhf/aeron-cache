/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.DirectBuffer;


/**
 * Mass update for the qtys at different prices on either side of the book for a single instrument
 */
@SuppressWarnings("all")
public final class MassSidedUpdateDecoder
{
    public static final int BLOCK_LENGTH = 183;
    public static final int TEMPLATE_ID = 5;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MassSidedUpdateDecoder parentMessage = this;
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
        return "MassSidedUpdate";
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

    public MassSidedUpdateDecoder wrap(
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

    public MassSidedUpdateDecoder wrapAndApplyHeader(
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

    private final SidedQuoteDecoder sidedQuote1 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote1()
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

    private final SidedQuoteDecoder sidedQuote2 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote2()
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

    private final SidedQuoteDecoder sidedQuote3 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote3()
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

    private final SidedQuoteDecoder sidedQuote4 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote4()
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

    private final SidedQuoteDecoder sidedQuote5 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote5()
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

    private final SidedQuoteDecoder sidedQuote6 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote6()
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

    private final SidedQuoteDecoder sidedQuote7 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote7()
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

    private final SidedQuoteDecoder sidedQuote8 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote8()
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

    private final SidedQuoteDecoder sidedQuote9 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote9()
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

    private final SidedQuoteDecoder sidedQuote10 = new SidedQuoteDecoder();

    public SidedQuoteDecoder sidedQuote10()
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

        final MassSidedUpdateDecoder decoder = new MassSidedUpdateDecoder();
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
        builder.append("[MassSidedUpdate](sbeTemplateId=");
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
        builder.append("sidedQuote1=");
        final SidedQuoteDecoder sidedQuote1 = sidedQuote1();
        if (sidedQuote1 != null)
        {
            sidedQuote1.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote2=");
        final SidedQuoteDecoder sidedQuote2 = sidedQuote2();
        if (sidedQuote2 != null)
        {
            sidedQuote2.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote3=");
        final SidedQuoteDecoder sidedQuote3 = sidedQuote3();
        if (sidedQuote3 != null)
        {
            sidedQuote3.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote4=");
        final SidedQuoteDecoder sidedQuote4 = sidedQuote4();
        if (sidedQuote4 != null)
        {
            sidedQuote4.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote5=");
        final SidedQuoteDecoder sidedQuote5 = sidedQuote5();
        if (sidedQuote5 != null)
        {
            sidedQuote5.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote6=");
        final SidedQuoteDecoder sidedQuote6 = sidedQuote6();
        if (sidedQuote6 != null)
        {
            sidedQuote6.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote7=");
        final SidedQuoteDecoder sidedQuote7 = sidedQuote7();
        if (sidedQuote7 != null)
        {
            sidedQuote7.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote8=");
        final SidedQuoteDecoder sidedQuote8 = sidedQuote8();
        if (sidedQuote8 != null)
        {
            sidedQuote8.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote9=");
        final SidedQuoteDecoder sidedQuote9 = sidedQuote9();
        if (sidedQuote9 != null)
        {
            sidedQuote9.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("sidedQuote10=");
        final SidedQuoteDecoder sidedQuote10 = sidedQuote10();
        if (sidedQuote10 != null)
        {
            sidedQuote10.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }

        limit(originalLimit);

        return builder;
    }
}
