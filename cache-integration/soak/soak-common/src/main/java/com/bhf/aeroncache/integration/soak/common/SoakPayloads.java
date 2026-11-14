package com.bhf.aeroncache.integration.soak.common;

import java.util.Random;

/**
 * Centralised key/value generation for every soak workload, so the payload size is one shared, size-aware seam
 * rather than scattered literals. Both the command/oracle soaks (opaque random-byte values) and the streaming
 * soaks (single-field JSON values) route all key and value generation through one instance of this class.
 *
 * <p>Sizes are driven from {@code -Psoak.keySizeBytes} / {@code -Psoak.valueSizeBytes}; {@code 0} means
 * "natural" and reproduces the pre-sizing behaviour exactly, so default runs are unchanged. Larger sizes let a
 * run exercise bigger key/value payloads against the cluster, gateway and interfaces.
 *
 * <p><b>Streaming invariant.</b> {@link #jsonValue(Random)} always produces a <em>single-field</em>
 * {@code {"v":...}} object. The streaming stack relies on this: a JSON merge patch between two single-field
 * objects is the whole new object, so a changed overwrite streams the entire value as its {@code PATCH_ITEM}
 * delta. Sizing the single string field up therefore grows the payload without disturbing any patch/verify
 * semantics - so large values work over streaming with no change to the verifier.
 */
public final class SoakPayloads {

    /** Pad character for sized keys. Not a member of any key token ("k", digits, d/s/f), so right-padding
     * never collapses two distinct tokens onto the same sized key. */
    private static final char KEY_PAD = '_';
    private static final char[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    /** Length of the {@code {"v":""}} scaffolding around the sized string in {@link #jsonValue(Random)}. */
    private static final int JSON_OVERHEAD = "{\"v\":\"\"}".length();

    private final int keySizeBytes;
    private final int valueSizeBytes;

    /**
     * @param keySizeBytes   target key length in bytes; {@code <= 0} leaves keys at their natural length. Keys
     *                       shorter than this are right-padded; keys already at/over it are left unchanged (keys
     *                       are never truncated, which would risk collisions).
     * @param valueSizeBytes target value length in bytes; {@code <= 0} leaves values at their natural form
     *                       (random alphabetic for commands, {@code {"v":N}} for streaming).
     */
    public SoakPayloads(int keySizeBytes, int valueSizeBytes) {
        this.keySizeBytes = keySizeBytes;
        this.valueSizeBytes = valueSizeBytes;
    }

    /**
     * The key for a logical token (e.g. {@code "0"}, {@code "1"}, {@code "d"}), as {@code "k" + token} padded to
     * {@link #keySizeBytes}. Distinct tokens always map to distinct keys.
     */
    public String key(String token) {
        var base = "k" + token;
        if (keySizeBytes <= base.length()) {
            return base;
        }
        var sb = new StringBuilder(keySizeBytes);
        sb.append(base);
        while (sb.length() < keySizeBytes) {
            sb.append(KEY_PAD);
        }
        return sb.toString();
    }

    /** An opaque random value of {@link #valueSizeBytes} alphabetic characters, for the command/oracle soaks. */
    public String randomValue(Random rnd) {
        return randomAlpha(rnd, Math.max(0, valueSizeBytes));
    }

    /**
     * A single-field {@code {"v":...}} JSON value for the streaming soaks. When sizing is off ({@code
     * valueSizeBytes <= 0}) this is {@code {"v":N}} with a random integer (the historical shape); otherwise it is
     * {@code {"v":"<random string>"}} whose total length is {@link #valueSizeBytes} (a floor of one inner char
     * applies for tiny sizes). It stays single-field either way, preserving merge-patch == whole-value.
     */
    public String jsonValue(Random rnd) {
        if (valueSizeBytes <= 0) {
            return "{\"v\":" + rnd.nextInt(1_000_000) + "}";
        }
        var inner = Math.max(1, valueSizeBytes - JSON_OVERHEAD);
        return "{\"v\":\"" + randomAlpha(rnd, inner) + "\"}";
    }

    private static String randomAlpha(Random rnd, int length) {
        var chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ALPHABET[rnd.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
