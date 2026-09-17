"use client"

import {useEffect, useState} from "react";

interface TimeRemainingProps {
    deadline: number
}

/**
 * Format a millisecond duration as a compact human readable string,
 * e.g. "1m 05s" or "2h 03m".
 */
function format(millis: number): string {
    if (millis <= 0) {
        return "due"
    }

    const totalSeconds = Math.floor(millis / 1000)
    const seconds = totalSeconds % 60
    const totalMinutes = Math.floor(totalSeconds / 60)
    const minutes = totalMinutes % 60
    const hours = Math.floor(totalMinutes / 60)

    const pad = (n: number) => n.toString().padStart(2, "0")

    if (hours > 0) {
        return hours + "h " + pad(minutes) + "m"
    }
    if (minutes > 0) {
        return minutes + "m " + pad(seconds) + "s"
    }
    return seconds + "s"
}

/**
 * A live-updating countdown to a timer's deadline.
 * @constructor
 */
export function TimeRemaining(props: TimeRemainingProps) {
    const [now, setNow] = useState<number>(() => Date.now())

    useEffect(() => {
        // Sync to the client clock immediately after hydration, then tick.
        setNow(Date.now())
        const interval = setInterval(() => setNow(Date.now()), 1000)
        return () => clearInterval(interval)
    }, [])

    const remaining = props.deadline - now

    // This is a live countdown driven by Date.now(), so the value rendered on
    // the server will differ from the value at client hydration a moment later.
    // Suppress the expected text/attribute mismatch for this node only.
    return (
        <span suppressHydrationWarning className={remaining <= 0 ? "text-red-600 font-medium" : ""}>
            {format(remaining)}
        </span>
    );
}
