package com.bhf.aeroncache.utils;

import org.agrona.concurrent.SystemEpochClock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class DNSUtils {
    public static void awaitDnsResolution(final List<String> hostArray, final int nodeId) {
        if (applyDnsDelay()) {
            System.out.println("Waiting 5 seconds for DNS to be registered...");
            quietSleep(5000);
        }

        final long endTime = SystemEpochClock.INSTANCE.time() + 60000;
        final String nodeName = hostArray.get(nodeId);
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");

        boolean resolved = false;
        while (!resolved) {
            if (SystemEpochClock.INSTANCE.time() > endTime) {
                System.out.println("cannot resolve name " + nodeName + ", exiting");
                System.exit(-1);
            }

            try {
                var res = InetAddress.getByName(nodeName);
                resolved = true;
                System.out.println("Resolved " + nodeName + " to " + res);
            } catch (final UnknownHostException e) {
                System.out.println("cannot yet resolve name " + nodeName + ", retrying in 3 seconds");
                quietSleep(3000);
            }
        }
    }

    /**
     * Sleeps for the given number of milliseconds, ignoring any interrupts.
     *
     * @param millis the number of milliseconds to sleep.
     */
    private static void quietSleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException ex) {
            System.out.println("Interrupted while sleeping");
        }
    }

    /**
     * Apply DNS delay
     *
     * @return true if DNS delay should be applied
     */
    private static boolean applyDnsDelay() {
        final String dnsDelay = System.getenv("DNS_DELAY");
        if (null == dnsDelay || dnsDelay.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(dnsDelay);
    }
}
