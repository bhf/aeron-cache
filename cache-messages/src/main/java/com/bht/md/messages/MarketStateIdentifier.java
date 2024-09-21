/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bht.md.messages;

public enum MarketStateIdentifier
{
    PRE_OPENING((short)0),

    OPENING_MODE((short)1),

    CONTINUOUS_TRADING_MODE((short)2),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL((short)255);

    private final short value;

    MarketStateIdentifier(final short value)
    {
        this.value = value;
    }

    /**
     * The raw encoded value in the Java type representation.
     *
     * @return the raw value encoded.
     */
    public short value()
    {
        return value;
    }

    /**
     * Lookup the enum value representing the value.
     *
     * @param value encoded to be looked up.
     * @return the enum value representing the value.
     */
    public static MarketStateIdentifier get(final short value)
    {
        switch (value)
        {
            case 0: return PRE_OPENING;
            case 1: return OPENING_MODE;
            case 2: return CONTINUOUS_TRADING_MODE;
            case 255: return NULL_VAL;
        }

        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
