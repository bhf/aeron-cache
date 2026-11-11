package com.bhf.aeroncache.integration.soak.common;

/**
 * Raises a soak failure: records the mismatch to the report (tagged with the current matrix cell) and returns an
 * {@link AssertionError} for the caller to throw. The command driver and stream verifier delegate here so every
 * failure, wherever it originates, lands in the report the same way.
 */
interface SoakFailures {

    AssertionError fail(String detail);
}
