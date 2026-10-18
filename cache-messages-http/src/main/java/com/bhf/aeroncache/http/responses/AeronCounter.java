package com.bhf.aeroncache.http.responses;

/**
 * A single Aeron media driver counter scraped from the driver's CnC file.
 *
 * <p>These are the driver's own counters (publications, subscriptions, flow control, errors, etc.),
 * not the cache application counters surfaced by {@link GetCountersResponse}.
 *
 * @param counterId the driver assigned counter id.
 * @param typeId    the counter type id identifying what the counter measures.
 * @param value     the current counter value.
 * @param label     the human readable counter label.
 */
public record AeronCounter(
        int counterId,
        int typeId,
        long value,
        String label) {
}
