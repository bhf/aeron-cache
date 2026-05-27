/* Generated SBE (Simple Binary Encoding) message codec. */
package com.bhf.aeroncache.messages;

@SuppressWarnings("all")
public enum BulkOperationType
{
    CREATE_CACHE((short)0),

    ADD_ITEM((short)1),

    REMOVE_ITEM((short)2),

    CLEAR_CACHE((short)3),

    GET_ITEM((short)4),

    DELETE_ITEM((short)5),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL((short)255);

    private final short value;

    BulkOperationType(final short value)
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
    public static BulkOperationType get(final short value)
    {
        switch (value)
        {
            case 0: return CREATE_CACHE;
            case 1: return ADD_ITEM;
            case 2: return REMOVE_ITEM;
            case 3: return CLEAR_CACHE;
            case 4: return GET_ITEM;
            case 5: return DELETE_ITEM;
            case 255: return NULL_VAL;
        }

        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
