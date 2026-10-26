/**
 * Observability model: which Aeron media driver counters the dashboard charts, how their labels are
 * matched across the clustered, ephemeral and monolith deployments, and how raw counter snapshots are
 * turned into chartable series.
 *
 * Counter labels differ subtly between deployments and Aeron versions, so every metric is matched by a
 * tolerant, case-insensitive regex over the counter label rather than by counter id. A metric that no
 * node reports (e.g. cluster and archive counters on an ephemeral deployment) is simply not charted.
 */

export interface AeronCounter {
    counterId: number
    typeId: number
    value: number
    label: string
}

export interface AeronCountersResponse {
    aeronDirectoryName: string
    timestamp: number
    count: number
    counters: AeronCounter[]
}

export interface NodeCounters {
    host: string
    reachable: boolean
    counters: AeronCountersResponse | null
}

export interface AeronCountersFanout {
    mode: "CLUSTERED" | "EPHEMERAL"
    nodeCount: number
    nodes: NodeCounters[]
}

export interface CacheStats {
    totalOpsCount: number
    totalCachesCount: number
    totalItemsCount: number
    errorCount: number
}

/** The payload served by the UI's /api/observability route. */
export interface ObservabilityPayload {
    timestamp: number
    stats: CacheStats | null
    counters: AeronCountersFanout | null
}

export type MetricGroup = "Transport" | "Cluster" | "Archive" | "Gateway"

export interface MetricDef {
    id: string
    group: MetricGroup
    title: string
    description: string
    /** "rate" charts the per second change of a monotonic counter; "gauge" charts the raw value. */
    kind: "rate" | "gauge"
    unit: string
    /** Matches the counter label. All matching counters on a node are summed. */
    pattern: RegExp
}

/** Aeron type ids registered by this project, see AeronCounterTracingService / AeronCounterGatewayMetrics. */
const CACHE_REQUEST_TYPE_ID = 2000
const GATEWAY_TYPE_ID = 2100

export const METRICS: MetricDef[] = [
    {id: "bytes-sent", group: "Transport", title: "Bytes sent", kind: "rate", unit: "B/s",
        description: "Media driver send throughput", pattern: /^bytes sent/i},
    {id: "bytes-received", group: "Transport", title: "Bytes received", kind: "rate", unit: "B/s",
        description: "Media driver receive throughput", pattern: /^bytes received/i},
    {id: "naks", group: "Transport", title: "NAKs", kind: "rate", unit: "/s",
        description: "Negative acknowledgements sent and received, a sign of packet loss", pattern: /^naks? (sent|received)/i},
    {id: "retransmits", group: "Transport", title: "Retransmits", kind: "rate", unit: "/s",
        description: "Frames retransmitted after loss", pattern: /^retransmits? sent|^retransmitted bytes/i},
    {id: "status-messages", group: "Transport", title: "Status messages", kind: "rate", unit: "/s",
        description: "Flow control status messages sent and received", pattern: /^status messages? (sent|received)/i},
    {id: "heartbeats", group: "Transport", title: "Heartbeats", kind: "rate", unit: "/s",
        description: "Heartbeats sent and received", pattern: /^heartbeats? (sent|received)/i},
    {id: "short-sends", group: "Transport", title: "Short sends", kind: "rate", unit: "/s",
        description: "Sends that did not write a full frame", pattern: /^short sends?/i},
    {id: "flow-control", group: "Transport", title: "Flow control over/under runs", kind: "rate", unit: "/s",
        description: "Receiver flow control window over and under runs", pattern: /^flow control (over|under) ?runs?/i},
    {id: "driver-errors", group: "Transport", title: "Driver errors", kind: "rate", unit: "/s",
        description: "Media driver and invalid packet errors", pattern: /^(errors|invalid packets|error frames received)\b/i},

    {id: "cluster-commit-pos", group: "Cluster", title: "Commit position", kind: "rate", unit: "B/s",
        description: "Rate of cluster log commit position advance", pattern: /^cluster[\s-]*commit[\s-]*pos/i},
    {id: "cluster-errors", group: "Cluster", title: "Cluster errors", kind: "gauge", unit: "",
        description: "Consensus module and clustered service errors",
        pattern: /^cluster[\s-]*(container[\s-]*)?errors?|^cluster[\s-]*module[\s-]*errors?|^consensus[\s-]*module[\s-]*errors?/i},
    {id: "cluster-timeouts", group: "Cluster", title: "Client timeouts", kind: "gauge", unit: "",
        description: "Cluster sessions that timed out",
        pattern: /^cluster[\s-]*(timed[\s-]*out[\s-]*client|client[\s-]*timeout)/i},
    {id: "cluster-sessions", group: "Cluster", title: "Open sessions", kind: "gauge", unit: "",
        description: "Open cluster client sessions", pattern: /^cluster[\s-]*(open[\s-]*)?(client[\s-]*)?sessions?/i},

    {id: "archive-control-sessions", group: "Archive", title: "Control sessions", kind: "gauge", unit: "",
        description: "Open archive control sessions", pattern: /^archive[\s-]*control[\s-]*sessions?/i},
    {id: "archive-recording-sessions", group: "Archive", title: "Recording sessions", kind: "gauge", unit: "",
        description: "Active archive recordings", pattern: /^archive[\s-]*recording[\s-]*sessions?/i},
    {id: "archive-replay-sessions", group: "Archive", title: "Replay sessions", kind: "gauge", unit: "",
        description: "Active archive replays", pattern: /^archive[\s-]*replay[\s-]*sessions?/i},
    {id: "archive-write-bytes", group: "Archive", title: "Archive write throughput", kind: "rate", unit: "B/s",
        description: "Bytes written to the archive", pattern: /^archive[\s-]*total[\s-]*write[\s-]*bytes/i},
    {id: "archive-write-time", group: "Archive", title: "Archive max write time", kind: "gauge", unit: "ns",
        description: "Longest archive write", pattern: /^archive[\s-]*max[\s-]*write[\s-]*time/i},
    {id: "archive-errors", group: "Archive", title: "Archive errors", kind: "gauge", unit: "",
        description: "Archive errors", pattern: /^archive[\s-]*errors?/i},

    {id: "gateway-sessions", group: "Gateway", title: "Sessions connected", kind: "gauge", unit: "",
        description: "Transport gateway sessions currently connected", pattern: /^AeronCache-Gateway-SessionsConnected/i},
    {id: "gateway-stream-updates", group: "Gateway", title: "Stream updates published", kind: "rate", unit: "/s",
        description: "Cache updates published to gateway subscribers", pattern: /^AeronCache-Gateway-StreamUpdatesPublished/i},
    {id: "gateway-requests", group: "Gateway", title: "Gateway requests", kind: "rate", unit: "/s",
        description: "Requests handled by the transport gateway", pattern: /^AeronCache-Gateway-Requests-/i},
]

/** A label to display for the cluster node role counter value. */
export const CLUSTER_ROLES: Record<number, string> = {0: "Follower", 1: "Candidate", 2: "Leader"}

const ROLE_PATTERN = /^cluster[\s-]*node[\s-]*role/i
const REQUEST_LABEL = /^AeronCache-Requests-(.+?)-node\S*$/i

/** One poll of the endpoints, reduced to the numbers the charts need. */
export interface Sample {
    t: number
    stats: CacheStats | null
    /** host -> metric id -> value. */
    nodes: Record<string, Record<string, number>>
    /** host -> cache operation -> request count (from the cache service tracing counters). */
    ops: Record<string, Record<string, number>>
    /** host -> cluster node role counter value. */
    roles: Record<string, number>
}

export function extractSample(payload: ObservabilityPayload): Sample {
    const sample: Sample = {t: payload.timestamp, stats: payload.stats, nodes: {}, ops: {}, roles: {}}
    for (const node of payload.counters?.nodes ?? []) {
        if (!node.reachable || !node.counters) continue
        const values: Record<string, number> = {}
        const ops: Record<string, number> = {}
        for (const c of node.counters.counters) {
            if (c.typeId === CACHE_REQUEST_TYPE_ID) {
                const op = REQUEST_LABEL.exec(c.label)?.[1]
                if (op) ops[op] = (ops[op] ?? 0) + c.value
                continue
            }
            if (ROLE_PATTERN.test(c.label)) {
                sample.roles[node.host] = c.value
                continue
            }
            for (const m of METRICS) {
                if (m.pattern.test(c.label) && (c.typeId !== GATEWAY_TYPE_ID || m.group === "Gateway")) {
                    values[m.id] = (values[m.id] ?? 0) + c.value
                }
            }
        }
        sample.nodes[node.host] = values
        sample.ops[node.host] = ops
    }
    return sample
}

export interface Row {
    t: number

    [series: string]: number
}

/**
 * Turn the sample history into chart rows, one series per key. Rates are the change since the previous
 * sample divided by the elapsed seconds; a counter that goes backwards (driver restart) reads as zero.
 */
export function buildRows(history: Sample[], kind: "rate" | "gauge",
                          read: (s: Sample) => Record<string, number | undefined>): Row[] {
    const rows: Row[] = []
    for (let i = kind === "rate" ? 1 : 0; i < history.length; i++) {
        const cur = read(history[i])
        const row: Row = {t: history[i].t}
        if (kind === "gauge") {
            for (const [k, v] of Object.entries(cur)) if (v !== undefined) row[k] = v
        } else {
            const prev = read(history[i - 1])
            const dt = (history[i].t - history[i - 1].t) / 1000
            if (dt <= 0) continue
            for (const [k, v] of Object.entries(cur)) {
                const p = prev[k]
                if (v !== undefined && p !== undefined) row[k] = Math.max(0, (v - p) / dt)
            }
        }
        rows.push(row)
    }
    return rows
}

/** Metric ids that at least one node has reported in the history, so absent metrics are not charted. */
export function reportedMetrics(history: Sample[]): Set<string> {
    const ids = new Set<string>()
    for (const s of history) for (const node of Object.values(s.nodes)) for (const id of Object.keys(node)) ids.add(id)
    return ids
}

/** A readable name for the deployment, derived from what the gateway reports. */
export function describeMode(counters: AeronCountersFanout | null): string {
    if (!counters) return "Unknown"
    if (counters.mode === "EPHEMERAL") return "Ephemeral"
    return counters.nodeCount <= 1 ? "Monolith / single node" : `Clustered (${counters.nodeCount} nodes)`
}

export function formatValue(v: number, unit: string): string {
    const abs = Math.abs(v)
    const steps: [number, string][] = unit === "B/s"
        ? [[1e9, "GB/s"], [1e6, "MB/s"], [1e3, "KB/s"]]
        : [[1e9, "G"], [1e6, "M"], [1e4, "k"]]
    for (const [size, suffix] of steps) {
        if (abs >= size) return `${(v / (suffix === "k" ? 1e3 : size)).toFixed(1)} ${suffix}${unit && unit !== "B/s" ? unit : ""}`.trim()
    }
    const text = Number.isInteger(v) ? String(v) : v.toFixed(abs < 10 ? 2 : 1)
    return `${text} ${unit}`.trim()
}
