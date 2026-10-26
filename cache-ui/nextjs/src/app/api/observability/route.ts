import {getCacheAPIURI} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import type {ObservabilityPayload} from "@/lib/observability";

const logger = getLogger("ObservabilityRoute")

export const dynamic = 'force-dynamic'

async function getJson<T>(url: string): Promise<T | null> {
    try {
        const response = await fetch(url, {headers: {'Accept': 'application/json'}, cache: "no-cache"})
        return response.ok ? await response.json() as T : null
    } catch (err) {
        logger.warn("Error whilst fetching " + url, err)
        return null
    }
}

/**
 * Serve the cache stats and the Aeron counters of every node, polled by the observability dashboard.
 * Either part is null when the gateway cannot provide it, so the dashboard can degrade gracefully.
 */
export async function GET() {
    const api = await getCacheAPIURI()
    const [stats, counters] = await Promise.all([
        getJson<ObservabilityPayload["stats"]>(api + '/stats/'),
        getJson<ObservabilityPayload["counters"]>(api + '/aeron-counters'),
    ])
    const payload: ObservabilityPayload = {timestamp: Date.now(), stats, counters}
    return Response.json(payload)
}
