/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

import org.agrona.MutableDirectBuffer;

@SuppressWarnings("all")
public final class QuoteSideEncoder
{
    public static final int SCHEMA_ID = 1;
    public static final int SCHEMA_VERSION = 0;
    public static final int ENCODED_LENGTH = 1;
    public static final java.nio.ByteOrder BYTE_ORDER = java.nio.ByteOrder.LITTLE_ENDIAN;

    private int offset;
    private MutableDirectBuffer buffer;

    public QuoteSideEncoder wrap(final MutableDirectBuffer buffer, final int offset)
    {
        if (buffer != this.buffer)
        {
            this.buffer = buffer;
        }
        this.offset = offset;

        return this;
    }

    public MutableDirectBuffer buffer()
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

    public QuoteSideEncoder clear()
    {
        buffer.putByte(offset, (byte)(short)0);
        return this;
    }

    public QuoteSideEncoder bid(final boolean value)
    {
        byte bits = buffer.getByte(offset);
        bits = (byte)(value ? bits | (1 << 0) : bits & ~(1 << 0));
        buffer.putByte(offset, bits);
        return this;
    }

    public static byte bid(final byte bits, final boolean value)
    {
        return (byte)(value ? bits | (1 << 0) : bits & ~(1 << 0));
    }

    public QuoteSideEncoder ask(final boolean value)
    {
        byte bits = buffer.getByte(offset);
        bits = (byte)(value ? bits | (1 << 1) : bits & ~(1 << 1));
        buffer.putByte(offset, bits);
        return this;
    }

    public static byte ask(final byte bits, final boolean value)
    {
        return (byte)(value ? bits | (1 << 1) : bits & ~(1 << 1));
    }
}
