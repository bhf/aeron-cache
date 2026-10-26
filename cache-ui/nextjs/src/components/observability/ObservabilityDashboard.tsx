"use client"

import {useEffect, useMemo, useState} from "react"
import {CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Badge} from "@/components/ui/badge"
import {
    buildRows,
    CLUSTER_ROLES,
    describeMode,
    extractSample,
    formatValue,
    METRICS,
    type MetricGroup,
    type ObservabilityPayload,
    reportedMetrics,
    type Row,
    type Sample,
} from "@/lib/observability"

/** Selectable poll intervals in ms; null pauses polling. */
const POLL_OPTIONS: { label: string, ms: number | null }[] = [
    {label: "1s", ms: 1000},
    {label: "2s", ms: 2000},
    {label: "5s", ms: 5000},
    {label: "10s", ms: 10000},
    {label: "30s", ms: 30000},
    {label: "Paused", ms: null},
]
const DEFAULT_POLL_MS = 2000
const MAX_SAMPLES = 150
const COLORS = ["#2563eb", "#16a34a", "#dc2626", "#d97706", "#7c3aed", "#0891b2", "#db2777", "#65a30d"]
const GROUPS: MetricGroup[] = ["Transport", "Cluster", "Archive", "Gateway"]

/**
 * Poll the observability route while mounted, keeping a bounded history of samples. Rates are computed
 * from sample timestamps, so the interval can change without distorting the charts.
 */
function useObservability(intervalMs: number | null) {
    const [history, setHistory] = useState<Sample[]>([])
    const [payload, setPayload] = useState<ObservabilityPayload | null>(null)
    const [error, setError] = useState(false)

    useEffect(() => {
        let cancelled = false
        const poll = async () => {
            try {
                const response = await fetch("/api/observability", {cache: "no-store"})
                const next = await response.json() as ObservabilityPayload
                if (cancelled) return
                setPayload(next)
                setHistory(h => [...h, extractSample(next)].slice(-MAX_SAMPLES))
                setError(false)
            } catch {
                if (!cancelled) setError(true)
            }
        }
        if (intervalMs === null) return
        poll()
        const timer = setInterval(poll, intervalMs)
        return () => {
            cancelled = true
            clearInterval(timer)
        }
    }, [intervalMs])

    return {history, payload, error}
}

/** Format a sample time as HH:mm:ss in the browser's timezone, identically for the axis and tooltip. */
const formatTime = (t: number) => new Date(t).toLocaleTimeString([], {hour12: false})

function TimeSeriesChart({rows, unit, height = 180}: { rows: Row[], unit: string, height?: number }) {
    const series = useMemo(() => {
        const keys = new Set<string>()
        rows.forEach(r => Object.keys(r).forEach(k => k !== "t" && keys.add(k)))
        return [...keys]
    }, [rows])

    if (rows.length < 2 || series.length === 0) {
        return <div className="text-sm text-muted-foreground" style={{height}}>Collecting data...</div>
    }
    return (
        <ResponsiveContainer width="100%" height={height}>
            <LineChart data={rows} margin={{top: 5, right: 8, bottom: 0, left: 0}}>
                <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.3}/>
                <XAxis dataKey="t" tick={{fontSize: 10}} minTickGap={40}
                       tickFormatter={formatTime}/>
                <YAxis tick={{fontSize: 10}} width={60} tickFormatter={v => formatValue(v, unit)}/>
                <Tooltip labelFormatter={t => formatTime(t as number)}
                         formatter={v => formatValue(v as number, unit)}/>
                {series.length > 1 && <Legend wrapperStyle={{fontSize: 11}}/>}
                {series.map((k, i) => (
                    <Line key={k} type="monotone" dataKey={k} stroke={COLORS[i % COLORS.length]}
                          dot={false} strokeWidth={2} isAnimationActive={false}/>
                ))}
            </LineChart>
        </ResponsiveContainer>
    )
}

function ChartCard({title, description, unit, rows, testId}: {
    title: string, description: string, unit: string, rows: Row[], testId: string
}) {
    return (
        <Card className="shadow-md py-4 gap-2" data-testid={testId}>
            <CardHeader className="px-4">
                <CardTitle className="text-base">{title}</CardTitle>
                <CardDescription className="text-xs">{description}</CardDescription>
            </CardHeader>
            <CardContent className="px-2"><TimeSeriesChart rows={rows} unit={unit}/></CardContent>
        </Card>
    )
}

function StatTile({label, value, testId}: { label: string, value: string | number | undefined, testId: string }) {
    return (
        <Card className="shadow-md py-4 gap-1" data-testid={testId}>
            <CardHeader className="px-4">
                <CardDescription>{label}</CardDescription>
                <CardTitle className="text-2xl font-semibold tabular-nums" data-testid={testId + "-value"}>
                    {value ?? "-"}
                </CardTitle>
            </CardHeader>
        </Card>
    )
}

function Section({title, children}: { title: string, children: React.ReactNode }) {
    return (
        <section className="space-y-3">
            <h3 className="text-lg font-semibold">{title}</h3>
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">{children}</div>
        </section>
    )
}

export default function ObservabilityDashboard() {
    const [intervalMs, setIntervalMs] = useState<number | null>(DEFAULT_POLL_MS)
    const {history, payload, error} = useObservability(intervalMs)
    const latest = history[history.length - 1]
    const reported = useMemo(() => reportedMetrics(history), [history])

    const cacheRows = useMemo(() => ({
        ops: buildRows(history, "rate", s => ({"ops/s": s.stats?.totalOpsCount})),
        items: buildRows(history, "gauge", s => ({items: s.stats?.totalItemsCount, caches: s.stats?.totalCachesCount})),
        errors: buildRows(history, "gauge", s => ({errors: s.stats?.errorCount})),
    }), [history])

    // Per operation request rate. Every cluster node processes every request, so take the highest
    // node's count rather than summing and over-reporting.
    const opRows = useMemo(() => buildRows(history, "rate", s => {
        const merged: Record<string, number> = {}
        for (const ops of Object.values(s.ops)) {
            for (const [op, v] of Object.entries(ops)) merged[op] = Math.max(merged[op] ?? 0, v)
        }
        return merged
    }), [history])
    const hasOps = opRows.some(r => Object.keys(r).length > 1)

    const unreachable = payload?.counters?.nodes.filter(n => !n.reachable).map(n => n.host) ?? []

    return (
        <div className="space-y-6" data-testid="observability-dashboard">
            <div className="flex flex-wrap items-center gap-2 text-sm">
                <Badge variant="outline" data-testid="observability-mode">{describeMode(payload?.counters ?? null)}</Badge>
                {Object.entries(latest?.roles ?? {}).map(([host, role]) => (
                    <Badge key={host} variant="secondary">{host}: {CLUSTER_ROLES[role] ?? `role ${role}`}</Badge>
                ))}
                {unreachable.length > 0 &&
                    <Badge variant="destructive">No counters from: {unreachable.join(", ")}</Badge>}
                {(error || (payload && !payload.stats)) && <Badge variant="destructive">Cache stats unavailable</Badge>}
                {payload && !payload.counters &&
                    <Badge variant="destructive">Aeron counters unavailable (is clustertools running?)</Badge>}
                <label className="ml-auto flex items-center gap-2 text-muted-foreground">
                    Refresh
                    <select className="rounded-md border bg-background px-2 py-1 text-foreground"
                            data-testid="observability-poll-rate"
                            value={intervalMs ?? "paused"}
                            onChange={e => setIntervalMs(e.target.value === "paused" ? null : Number(e.target.value))}>
                        {POLL_OPTIONS.map(o => (
                            <option key={o.label} value={o.ms ?? "paused"}>{o.label}</option>
                        ))}
                    </select>
                </label>
            </div>

            <section className="space-y-3">
                <h3 className="text-lg font-semibold">Cache</h3>
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <StatTile label="Total Ops" value={latest?.stats?.totalOpsCount} testId="stats-total-ops"/>
                    <StatTile label="Caches" value={latest?.stats?.totalCachesCount} testId="stats-total-caches"/>
                    <StatTile label="Items" value={latest?.stats?.totalItemsCount} testId="stats-total-items"/>
                    <StatTile label="Errors" value={latest?.stats?.errorCount} testId="stats-total-errors"/>
                </div>
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                    <ChartCard title="Operations / sec" description="Cache operations handled by the gateway"
                               unit="/s" rows={cacheRows.ops} testId="chart-cache-ops"/>
                    <ChartCard title="Caches and items" description="Current number of caches and items"
                               unit="" rows={cacheRows.items} testId="chart-cache-items"/>
                    <ChartCard title="Errors" description="Cumulative cache errors" unit=""
                               rows={cacheRows.errors} testId="chart-cache-errors"/>
                    {hasOps && <ChartCard title="Requests by operation / sec"
                                          description="Cache service request counters, by operation"
                                          unit="/s" rows={opRows} testId="chart-cache-requests"/>}
                </div>
            </section>

            {GROUPS.map(group => {
                const metrics = METRICS.filter(m => m.group === group && reported.has(m.id))
                if (metrics.length === 0) return null
                return (
                    <Section key={group} title={group === "Transport" ? "Aeron transport (media driver)" : `Aeron ${group.toLowerCase()}`}>
                        {metrics.map(m => (
                            <ChartCard key={m.id} title={m.title} description={m.description} unit={m.unit}
                                       testId={"chart-" + m.id}
                                       rows={buildRows(history, m.kind, s => {
                                           const out: Record<string, number | undefined> = {}
                                           for (const [host, values] of Object.entries(s.nodes)) out[host] = values[m.id]
                                           return out
                                       })}/>
                        ))}
                    </Section>
                )
            })}

            {reported.size > 0 && !METRICS.some(m => m.group === "Cluster" && reported.has(m.id)) && (
                <p className="text-sm text-muted-foreground">
                    No Aeron cluster counters were reported, which is expected for an ephemeral deployment.
                </p>
            )}
        </div>
    )
}
