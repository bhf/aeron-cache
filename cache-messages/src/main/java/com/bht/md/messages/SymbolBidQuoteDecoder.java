/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.DirectBuffer;

@SuppressWarnings("all")
public final class SymbolBidQuoteDecoder
{
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final int ENCODED_LENGTH = 17;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private int offset;
    private DirectBuffer buffer;

    public SymbolBidQuoteDecoder wrap(final DirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;

        return this;
    }

    public DirectBuffer buffer()
    {
        return buffer;
    }

    public int offset()
    {
        return offset;
    }

    public int encodedLength()
    {
        return ENCODED_LENGTH;
    }

    public int sbeSchemaId()
    {
        return SCHEMA_ID;
    }

    public int sbeSchemaVersion()
    {
        return SCHEMA_VERSION;
    }

    public static int symbEncodingOffset()
    {
        return 0;
    }

    public static int symbEncodingLength()
    {
        return 1;
    }

    public static int symbSinceVersion()
    {
        return 0;
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
        return ((short)(buffer.getByte(offset + 0) & 0xFF));
    }


    public static int priceEncodingOffset()
    {
        return 1;
    }

    public static int priceEncodingLength()
    {
        return 8;
    }

    public static int priceSinceVersion()
    {
        return 0;
    }

    public static long priceNullValue()
    {
        return -9223372036854775808L;
    }

    public static long priceMinValue()
    {
        return -9223372036854775807L;
    }

    public static long priceMaxValue()
    {
        return 9223372036854775807L;
    }

    public long price()
    {
        return buffer.getLong(offset + 1, java.nio.ByteOrder.LITTLE_ENDIAN);
    }


    public static int qtyEncodingOffset()
    {
        return 9;
    }

    public static int qtyEncodingLength()
    {
        return 8;
    }

    public static int qtySinceVersion()
    {
        return 0;
    }

    public static long qtyNullValue()
    {
        return -9223372036854775808L;
    }

    public static long qtyMinValue()
    {
        return -9223372036854775807L;
    }

    public static long qtyMaxValue()
    {
        return 9223372036854775807L;
    }

    public long qty()
    {
        return buffer.getLong(offset + 9, java.nio.ByteOrder.LITTLE_ENDIAN);
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

        builder.append('(');
        builder.append("symb=");
        builder.append(symb());
        builder.append('|');
        builder.append("price=");
        builder.append(price());
        builder.append('|');
        builder.append("qty=");
        builder.append(qty());
        builder.append(')');

        return builder;
    }
}
