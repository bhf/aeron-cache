package com.bhf.aeroncache.integration.soak.common;

import java.util.Random;

/**
 * Helpers for reading {@code -Psoak.*} run parameters (forwarded as system properties by the module
 * build) with defaults. Shared by every soak suite's configuration. Blank values are treated as "unset"
 * so an empty dispatch input falls back to the default.
 */
public final class SoakProps {

    private SoakProps() {
    }

    /** Resolves a seed: an explicit value if provided, otherwise a fresh random one (to be logged). */
    public static long seed(String name) {
        var raw = System.getProperty(name);
        if (raw == null || raw.isBlank()) {
            return new Random().nextLong();
        }
        return Long.parseLong(raw.trim());
    }

    public static long longProp(String name, long dflt) {
        var raw = System.getProperty(name);
        return (raw == null || raw.isBlank()) ? dflt : Long.parseLong(raw.trim());
    }

    public static int intProp(String name, int dflt) {
        var raw = System.getProperty(name);
        return (raw == null || raw.isBlank()) ? dflt : Integer.parseInt(raw.trim());
    }

    public static boolean boolProp(String name, boolean dflt) {
        var raw = System.getProperty(name);
        return (raw == null || raw.isBlank()) ? dflt : Boolean.parseBoolean(raw.trim());
    }
}
