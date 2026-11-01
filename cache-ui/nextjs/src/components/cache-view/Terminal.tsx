"use client"

import {useEffect, useMemo, useRef, useState} from "react"
import {ChevronRight, Loader2, Radio, TriangleAlert} from "lucide-react"
import {cn} from "@/lib/utils"
import {highlightJson, tryFormatJson} from "@/components/cache-view/JsonViewer"

interface TerminalProps {
    messages?: string[],
    isConnected?: boolean,
    errorMsg?: string | null,
    title?: string,
    subtitle?: string,
}

/** Keep the rendered feed bounded so a long-lived stream stays responsive. */
const MAX_VISIBLE = 200

type Tone = "success" | "error" | "warn" | "info" | "neutral"

/** A single parsed event, derived from the raw JSON string handed to us. */
interface Event {
    index: number
    receivedAt: number
    obj: Record<string, unknown>
    tone: Tone
    badge?: string
    raw: string
}

const TONE_ACCENT: Record<Tone, string> = {
    success: "border-l-emerald-400",
    error: "border-l-rose-400",
    warn: "border-l-amber-400",
    info: "border-l-sky-400",
    neutral: "border-l-zinc-600",
}

const TONE_BADGE: Record<Tone, string> = {
    success: "bg-emerald-400/10 text-emerald-300 ring-emerald-400/20",
    error: "bg-rose-400/10 text-rose-300 ring-rose-400/20",
    warn: "bg-amber-400/10 text-amber-300 ring-amber-400/20",
    info: "bg-sky-400/10 text-sky-300 ring-sky-400/20",
    neutral: "bg-zinc-400/10 text-zinc-300 ring-zinc-400/20",
}

/** Maps a cache update event type to a display label and colour tone. */
const EVENT_META: Record<string, { tone: Tone; label: string }> = {
    ADD_ITEM: {tone: "success", label: "ADD"},
    PATCH_ITEM: {tone: "info", label: "PATCH"},
    REMOVE_ITEM: {tone: "warn", label: "REMOVE"},
    CLEAR_CACHE: {tone: "error", label: "CLEAR"},
    DELETE_CACHE: {tone: "error", label: "DELETE"},
}

/** Falls back to operationStatus/status for payloads that aren't cache update events. */
function toneForStatus(status: string): Tone {
    const s = status.toUpperCase()
    if (/SUCCESS|\bOK\b|DONE|COMPLETE|CREATED|STORED|UPDATED/.test(s)) return "success"
    if (/ERROR|FAIL|DENIED|INVALID|TIMEOUT|LOST|MISSING|NOT_FOUND/.test(s)) return "error"
    return "info"
}

/** Derives the row's badge label and tone from an event's fields. */
function badgeFor(obj: Record<string, unknown>): { tone: Tone; label?: string } {
    const eventType = obj.eventType as string | undefined
    if (eventType) {
        return EVENT_META[eventType.toUpperCase()] ?? {tone: "info", label: eventType}
    }
    const status = (obj.operationStatus ?? obj.status) as string | undefined
    if (status) {
        return {tone: toneForStatus(status), label: status}
    }
    return {tone: "neutral"}
}

/** Format an arrival time as HH:MM:SS.mmm for a dense, log-like timeline. */
function formatTime(ts: number): string {
    const d = new Date(ts)
    const hms = d.toLocaleTimeString([], {hour12: false})
    return `${hms}.${d.getMilliseconds().toString().padStart(3, "0")}`
}

function parseEvent(raw: string, index: number, receivedAt: number): Event {
    let obj: Record<string, unknown>
    try {
        const parsed = JSON.parse(raw)
        obj = parsed && typeof parsed === "object" ? parsed : {value: parsed}
    } catch {
        obj = {message: raw}
    }
    const {tone, label} = badgeFor(obj)
    return {index, receivedAt, obj, tone, badge: label, raw}
}

/** A compact one-line summary of an event's most meaningful fields. */
function EventSummary({obj}: { obj: Record<string, unknown> }) {
    const key = (obj.itemKey ?? obj.key) as unknown
    const value = (obj.itemValue ?? obj.value) as unknown
    const message = (obj.message ?? obj.msg ?? obj.data) as unknown

    if (key !== undefined && key !== null) {
        const hasValue = value !== undefined && value !== null
        const isPrimitive = hasValue && typeof value !== "object"
        return (
            <span className="min-w-0 truncate">
                <span className="text-sky-300">{String(key)}</span>
                {hasValue && (
                    <>
                        <span className="px-1.5 text-zinc-500">→</span>
                        <span className={isPrimitive && typeof value === "number" ? "text-emerald-300" : "text-amber-300"}>
                            {isPrimitive ? String(value) : JSON.stringify(value)}
                        </span>
                    </>
                )}
            </span>
        )
    }

    if (typeof message === "string" && message.length > 0) {
        return <span className="min-w-0 truncate text-zinc-300">{message}</span>
    }

    // Cache-level events (CLEAR_CACHE / DELETE_CACHE) carry no item — surface the cache id.
    if (obj.cacheId) {
        return <span className="min-w-0 truncate text-zinc-400">{String(obj.cacheId)}</span>
    }

    return <span className="min-w-0 truncate text-zinc-400">{JSON.stringify(obj)}</span>
}

function EventRow({event}: { event: Event }) {
    const [open, setOpen] = useState(false)
    const formatted = useMemo(() => tryFormatJson(event.raw), [event.raw])

    return (
        <div
            className={cn(
                "group rounded-md border-l-2 pl-3 pr-2 py-1.5 transition-colors hover:bg-white/[0.04]",
                "animate-in fade-in slide-in-from-bottom-1 duration-300",
                TONE_ACCENT[event.tone],
            )}
        >
            <div className="flex items-center gap-3">
                <time className="shrink-0 tabular-nums text-[11px] text-zinc-500">
                    {formatTime(event.receivedAt)}
                </time>

                {event.badge && (
                    <span
                        className={cn(
                            "shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide ring-1 ring-inset",
                            TONE_BADGE[event.tone],
                        )}
                    >
                        {event.badge}
                    </span>
                )}

                <div className="flex-1 overflow-hidden">
                    <EventSummary obj={event.obj}/>
                </div>

                {formatted && (
                    <button
                        type="button"
                        onClick={() => setOpen((v) => !v)}
                        aria-expanded={open}
                        aria-label={open ? "Hide payload" : "Show payload"}
                        className="shrink-0 rounded p-0.5 text-zinc-500 opacity-0 transition hover:bg-white/10 hover:text-zinc-200 focus-visible:opacity-100 group-hover:opacity-100"
                    >
                        <ChevronRight size={14} className={cn("transition-transform", open && "rotate-90")}/>
                    </button>
                )}
            </div>

            {open && formatted && (
                <pre
                    className="mt-1.5 overflow-x-auto rounded-md bg-black/40 p-3 text-[11px] leading-relaxed text-zinc-300 ring-1 ring-white/5"
                    dangerouslySetInnerHTML={{__html: highlightJson(formatted)}}
                />
            )}
        </div>
    )
}

export default function Terminal({
                                     messages,
                                     isConnected,
                                     errorMsg,
                                     title = "aeron-cache",
                                     subtitle = "live stream",
                                 }: TerminalProps) {

    const count = messages?.length ?? 0

    // Capture an arrival time for each message as it first appears, so the feed
    // shows a real timeline rather than relying on fields that may be absent.
    const [times, setTimes] = useState<number[]>([])
    useEffect(() => {
        setTimes((prev) => {
            if (count > prev.length) {
                const now = Date.now()
                return [...prev, ...Array(count - prev.length).fill(now)]
            }
            if (count < prev.length) {
                return prev.slice(0, count)
            }
            return prev
        })
    }, [count])

    const events = useMemo(() => {
        if (!messages) return []
        const start = Math.max(0, messages.length - MAX_VISIBLE)
        return messages
            .slice(start)
            .map((raw, i) => parseEvent(raw, start + i, times[start + i] ?? Date.now()))
    }, [messages, times])

    // Auto-scroll to the newest event, but only while the user is already at the
    // bottom — so scrolling up to read history isn't yanked away.
    const scrollRef = useRef<HTMLDivElement>(null)
    const pinnedRef = useRef(true)
    useEffect(() => {
        const el = scrollRef.current
        if (el && pinnedRef.current) {
            el.scrollTop = el.scrollHeight
        }
    }, [count])

    const onScroll = () => {
        const el = scrollRef.current
        if (!el) return
        pinnedRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 48
    }

    return (
        <div className="flex h-[calc(100vh-5rem)] w-full flex-col overflow-hidden rounded-xl bg-zinc-950 font-mono text-sm text-zinc-100 shadow-2xl ring-1 ring-white/10">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-white/10 bg-zinc-900/60 px-4 py-2.5 backdrop-blur">
                <div className="flex items-center gap-2.5">
                    <Radio size={16} className="text-aeroncache"/>
                    <div className="flex items-baseline gap-2">
                        <span className="text-sm font-semibold text-zinc-200">{title}</span>
                        <span className="text-xs text-zinc-500">{subtitle}</span>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <span className="hidden text-xs tabular-nums text-zinc-500 sm:inline">
                        {count} {count === 1 ? "event" : "events"}
                    </span>
                    <span
                        className={cn(
                            "flex items-center gap-2 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset",
                            isConnected
                                ? "bg-emerald-400/10 text-emerald-300 ring-emerald-400/20"
                                : "bg-rose-400/10 text-rose-300 ring-rose-400/20",
                        )}
                    >
                        <span className="relative flex h-2 w-2">
                            {isConnected && (
                                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"/>
                            )}
                            <span
                                className={cn(
                                    "relative inline-flex h-2 w-2 rounded-full",
                                    isConnected ? "bg-emerald-400" : "bg-rose-400",
                                )}
                            />
                        </span>
                        {isConnected ? "Connected" : "Offline"}
                    </span>
                </div>
            </div>

            {/* Error banner */}
            {errorMsg && (
                <div className="flex items-center gap-2 border-b border-rose-400/20 bg-rose-500/10 px-4 py-2 text-xs text-rose-300">
                    <TriangleAlert size={14} className="shrink-0"/>
                    <span className="truncate">{errorMsg}</span>
                </div>
            )}

            {/* Feed */}
            <div ref={scrollRef} onScroll={onScroll} className="flex-1 overflow-y-auto px-2 py-2">
                {events.length === 0 ? (
                    <div className="flex h-full flex-col items-center justify-center gap-3 text-zinc-500">
                        <Radio size={28} className="animate-pulse"/>
                        <p className="text-sm">Waiting for the first update…</p>
                    </div>
                ) : (
                    <div className="space-y-0.5">
                        {events.map((event) => (
                            <EventRow key={event.index} event={event}/>
                        ))}
                    </div>
                )}
            </div>

            {/* Status footer */}
            <div className="flex items-center gap-2 border-t border-white/10 bg-zinc-900/60 px-4 py-2 text-[11px] text-zinc-500">
                {isConnected ? (
                    <>
                        <Loader2 size={12} className="animate-spin text-zinc-500"/>
                        <span>Listening for updates</span>
                    </>
                ) : (
                    <>
                        <TriangleAlert size={12} className="text-rose-400/80"/>
                        <span>Reconnecting…</span>
                    </>
                )}
                <span className="ml-auto tabular-nums">
                    {count} {count === 1 ? "event" : "events"}
                </span>
            </div>
        </div>
    )
}
