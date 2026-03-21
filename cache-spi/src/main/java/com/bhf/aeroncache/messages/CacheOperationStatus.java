/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

public enum CacheOperationStatus
{
    NONE((short)0),

    SUCCESS((short)1),

    ERROR((short)2),

    UNKNOWN_CACHE((short)3),

    UNKNOWN_KEY((short)4),

    CACHE_EXISTS((short)5),

    DUPLICATE_SUBSCRIPTION((short)6),

    UNKNOWN_SUBSCRIPTION((short)7),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL((short)255);

    private final short value;

    CacheOperationStatus(final short value)
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
    public static CacheOperationStatus get(final short value)
    {
        switch (value)
        {
            case 0: return NONE;
            case 1: return SUCCESS;
            case 2: return ERROR;
            case 3: return UNKNOWN_CACHE;
            case 4: return UNKNOWN_KEY;
            case 5: return CACHE_EXISTS;
            case 6: return DUPLICATE_SUBSCRIPTION;
            case 7: return UNKNOWN_SUBSCRIPTION;
            case 255: return NULL_VAL;
        }

        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
