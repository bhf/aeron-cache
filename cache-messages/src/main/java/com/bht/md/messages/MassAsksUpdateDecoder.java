/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.DirectBuffer;


/**
 * Mass update for the qtys at different prices on the ask side of the book for a single instrument
 */
@SuppressWarnings("all")
public final class MassAsksUpdateDecoder
{
    public static final int BLOCK_LENGTH = 173;
    public static final int TEMPLATE_ID = 4;
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private final MassAsksUpdateDecoder parentMessage = this;
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
        return "MassAsksUpdate";
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

    public MassAsksUpdateDecoder wrap(
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

    public MassAsksUpdateDecoder wrapAndApplyHeader(
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
        return 13;
    }

    public static int askQuote1EncodingLength()
    {
        return 16;
    }

    public static String askQuote1MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote1 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote1()
    {
        askQuote1.wrap(buffer, offset + 13);
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
        return 16;
    }

    public static String askQuote2MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote2 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote2()
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
        return 45;
    }

    public static int askQuote3EncodingLength()
    {
        return 16;
    }

    public static String askQuote3MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote3 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote3()
    {
        askQuote3.wrap(buffer, offset + 45);
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
        return 61;
    }

    public static int askQuote4EncodingLength()
    {
        return 16;
    }

    public static String askQuote4MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote4 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote4()
    {
        askQuote4.wrap(buffer, offset + 61);
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
        return 77;
    }

    public static int askQuote5EncodingLength()
    {
        return 16;
    }

    public static String askQuote5MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote5 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote5()
    {
        askQuote5.wrap(buffer, offset + 77);
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
        return 93;
    }

    public static int askQuote6EncodingLength()
    {
        return 16;
    }

    public static String askQuote6MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote6 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote6()
    {
        askQuote6.wrap(buffer, offset + 93);
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
        return 109;
    }

    public static int askQuote7EncodingLength()
    {
        return 16;
    }

    public static String askQuote7MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote7 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote7()
    {
        askQuote7.wrap(buffer, offset + 109);
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
        return 125;
    }

    public static int askQuote8EncodingLength()
    {
        return 16;
    }

    public static String askQuote8MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote8 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote8()
    {
        askQuote8.wrap(buffer, offset + 125);
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
        return 141;
    }

    public static int askQuote9EncodingLength()
    {
        return 16;
    }

    public static String askQuote9MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote9 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote9()
    {
        askQuote9.wrap(buffer, offset + 141);
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
        return 157;
    }

    public static int askQuote10EncodingLength()
    {
        return 16;
    }

    public static String askQuote10MetaAttribute(final MetaAttribute metaAttribute)
    {
        if (MetaAttribute.PRESENCE == metaAttribute)
        {
            return "optional";
        }

        return "";
    }

    private final AskQuoteDecoder askQuote10 = new AskQuoteDecoder();

    public AskQuoteDecoder askQuote10()
    {
        askQuote10.wrap(buffer, offset + 157);
        return askQuote10;
    }

    public String toString()
    {
        if (null == buffer)
        {
            return "";
        }

        final MassAsksUpdateDecoder decoder = new MassAsksUpdateDecoder();
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
        builder.append("[MassAsksUpdate](sbeTemplateId=");
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
        builder.append("askQuote1=");
        final AskQuoteDecoder askQuote1 = askQuote1();
        if (askQuote1 != null)
        {
            askQuote1.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote2=");
        final AskQuoteDecoder askQuote2 = askQuote2();
        if (askQuote2 != null)
        {
            askQuote2.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote3=");
        final AskQuoteDecoder askQuote3 = askQuote3();
        if (askQuote3 != null)
        {
            askQuote3.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote4=");
        final AskQuoteDecoder askQuote4 = askQuote4();
        if (askQuote4 != null)
        {
            askQuote4.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote5=");
        final AskQuoteDecoder askQuote5 = askQuote5();
        if (askQuote5 != null)
        {
            askQuote5.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote6=");
        final AskQuoteDecoder askQuote6 = askQuote6();
        if (askQuote6 != null)
        {
            askQuote6.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote7=");
        final AskQuoteDecoder askQuote7 = askQuote7();
        if (askQuote7 != null)
        {
            askQuote7.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote8=");
        final AskQuoteDecoder askQuote8 = askQuote8();
        if (askQuote8 != null)
        {
            askQuote8.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote9=");
        final AskQuoteDecoder askQuote9 = askQuote9();
        if (askQuote9 != null)
        {
            askQuote9.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }
        builder.append('|');
        builder.append("askQuote10=");
        final AskQuoteDecoder askQuote10 = askQuote10();
        if (askQuote10 != null)
        {
            askQuote10.appendTo(builder);
        }
        else
        {
            builder.append("null");
        }

        limit(originalLimit);

        return builder;
    }
}
