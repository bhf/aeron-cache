/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

public enum OperationStatus
{
    NONE((short)0),

    SUCCESS((short)1),

    ERROR((short)2),

    UNKNOWN_CACHE((short)3),

    UNKNOWN_KEY((short)4),

    CACHE_EXISTS((short)5),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL((short)255);

    private final short value;

    OperationStatus(final short value)
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
    public static OperationStatus get(final short value)
    {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> SUCCESS;
            case 2 -> ERROR;
            case 3 -> UNKNOWN_CACHE;
            case 4 -> UNKNOWN_KEY;
            case 5 -> CACHE_EXISTS;
            case 255 -> NULL_VAL;
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };

    }
}
