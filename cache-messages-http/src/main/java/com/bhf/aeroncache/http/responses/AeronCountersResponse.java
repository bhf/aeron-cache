package com.bhf.aeroncache.http.responses;

import java.util.List;

/**
 * Snapshot of the Aeron media driver counters scraped from a driver's CnC file.
 *
 * <p>Reports the driver's own counters, not the cache application counters.
 *
 * @param aeronDirectoryName the Aeron media driver directory the counters were read from.
 * @param cncVersion         the CnC file format version the driver published.
 * @param timestamp          epoch millis when the counters were scraped.
 * @param count              the number of allocated counters returned.
 * @param counters           the scraped counters.
 */
public record AeronCountersResponse(
        String aeronDirectoryName,
        int cncVersion,
        long timestamp,
        int count,
        List<AeronCounter> counters) {
}
