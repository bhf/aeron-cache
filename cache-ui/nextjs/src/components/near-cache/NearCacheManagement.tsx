"use client";

import {useCallback, useEffect, useState, useTransition} from "react";
import {toast} from "sonner";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "@/components/ui/table";
import {CirclePlayIcon, CircleStopIcon, FlameIcon, RefreshCwIcon} from "lucide-react";
import {listWarmNearCaches, primeNearCache, stopNearCache} from "@/lib/actions";
import {getLogger} from "@/lib/loggingUtil";
import type {NearCacheGateway, NearCacheKind, WarmCacheInfo} from "@/lib/types";

const logger = getLogger("NearCacheManagement");

/** The gateways that can be managed, in display order. */
const GATEWAYS: { id: NearCacheGateway; label: string }[] = [
    {id: "http", label: "HTTP"},
    {id: "ws", label: "WebSocket"},
    {id: "sse", label: "SSE"},
    {id: "aeron", label: "Aeron"},
];

/** The cache kinds that can be managed. */
const KINDS: { id: NearCacheKind; label: string }[] = [
    {id: "caches", label: "Caches"},
    {id: "counters", label: "Counter Caches"},
];

/**
 * Near cache management for every gateway. Each gateway keeps its own independent near cache, so the
 * view is scoped to one gateway and one cache kind (regular caches or counter caches) at a time. From
 * here the user can see which caches are currently warm (and how many items each holds), prime a cache
 * so it is warmed up front, and stop near caching a cache to drop its local copy.
 */
export default function NearCacheManagement() {
    const [gateway, setGateway] = useState<NearCacheGateway>("http");
    const [kind, setKind] = useState<NearCacheKind>("caches");
    const [warmCaches, setWarmCaches] = useState<WarmCacheInfo[]>([]);
    const [loading, setLoading] = useState(false);
    const [primeId, setPrimeId] = useState("");
    const [isPending, startTransition] = useTransition();

    const gatewayLabel = GATEWAYS.find((g) => g.id === gateway)?.label ?? gateway;
    const kindLabel = kind === "counters" ? "counter cache" : "cache";

    const refresh = useCallback(async () => {
        setLoading(true);
        try {
            const result = await listWarmNearCaches(gateway, kind);
            setWarmCaches(result);
        } catch (err) {
            logger.warn("Failed to load warm near caches", err);
            setWarmCaches([]);
        } finally {
            setLoading(false);
        }
    }, [gateway, kind]);

    // Reload the warm cache list whenever the selected gateway or kind changes.
    useEffect(() => {
        void refresh();
    }, [refresh]);

    const handlePrime = () => {
        const cacheId = primeId.trim();
        if (cacheId.length === 0) {
            toast("Enter a cache ID", {
                description: "Type the ID of the " + kindLabel + " to prime",
                action: {label: "OK", onClick: () => {}},
            });
            return;
        }
        startTransition(async () => {
            const result = await primeNearCache(gateway, kind, cacheId);
            if (result.error) {
                toast.error("Prime failed", {description: result.message, action: {label: "OK", onClick: () => {}}});
                return;
            }
            toast.success("Prime " + gatewayLabel, {description: result.message, action: {label: "OK", onClick: () => {}}});
            setPrimeId("");
            await refresh();
        });
    };

    const handleStop = (cacheId: string) => {
        startTransition(async () => {
            const result = await stopNearCache(gateway, kind, cacheId);
            if (result.error) {
                toast.error("Stop failed", {description: result.message, action: {label: "OK", onClick: () => {}}});
                return;
            }
            toast.success("Stop " + gatewayLabel, {description: result.message, action: {label: "OK", onClick: () => {}}});
            await refresh();
        });
    };

    const busy = loading || isPending;

    return (
        <div className="space-y-6" data-testid="near-cache-management">
            <Card className="shadow-lg">
                <CardHeader>
                    <CardTitle>Gateway</CardTitle>
                    <CardDescription>
                        Each gateway keeps its own near cache. Pick a gateway and cache kind to manage.
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="flex flex-wrap gap-2" data-testid="near-gateway-selector">
                        {GATEWAYS.map((g) => (
                            <Button
                                key={g.id}
                                variant={gateway === g.id ? "default" : "outline"}
                                size="sm"
                                onClick={() => setGateway(g.id)}
                                disabled={busy}
                                data-testid={`near-gateway-${g.id}`}
                            >
                                {g.label}
                            </Button>
                        ))}
                    </div>
                    <div className="flex flex-wrap gap-2" data-testid="near-kind-selector">
                        {KINDS.map((k) => (
                            <Button
                                key={k.id}
                                variant={kind === k.id ? "secondary" : "outline"}
                                size="sm"
                                onClick={() => setKind(k.id)}
                                disabled={busy}
                                data-testid={`near-kind-${k.id}`}
                            >
                                {k.label}
                            </Button>
                        ))}
                    </div>
                </CardContent>
            </Card>

            <Card className="shadow-lg">
                <CardHeader>
                    <CardTitle>Prime a {kindLabel}</CardTitle>
                    <CardDescription>
                        Warm a {kindLabel} up front on the {gatewayLabel} gateway so reads are served locally.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="flex flex-wrap gap-2">
                        <Input
                            placeholder="Cache ID"
                            value={primeId}
                            onChange={(e) => setPrimeId(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === "Enter") {
                                    e.preventDefault();
                                    handlePrime();
                                }
                            }}
                            className="max-w-xs"
                            disabled={busy}
                            data-testid="near-prime-input"
                        />
                        <Button onClick={handlePrime} disabled={busy} data-testid="near-prime-button">
                            <CirclePlayIcon/>Prime
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <Card className="shadow-lg">
                <CardHeader>
                    <div className="flex items-center justify-between gap-2">
                        <div>
                            <CardTitle className="flex items-center gap-2">
                                <FlameIcon className="size-4 text-orange-500"/>
                                Warm {kind === "counters" ? "Counter Caches" : "Caches"}
                            </CardTitle>
                            <CardDescription>
                                {gatewayLabel} gateway &middot; {warmCaches.length} warm
                            </CardDescription>
                        </div>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => void refresh()}
                            disabled={busy}
                            data-testid="near-refresh-button"
                        >
                            <RefreshCwIcon className={loading ? "animate-spin" : ""}/>Refresh
                        </Button>
                    </div>
                </CardHeader>
                <CardContent>
                    <div className="rounded-md border" data-testid="near-warm-table">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Cache ID</TableHead>
                                    <TableHead>Item Count</TableHead>
                                    <TableHead className="text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {warmCaches.length > 0 ? (
                                    warmCaches.map((c) => (
                                        <TableRow key={c.cacheId} data-testid={`near-warm-row-${c.cacheId}`}>
                                            <TableCell className="font-medium">{c.cacheId}</TableCell>
                                            <TableCell className="tabular-nums">{c.itemCount}</TableCell>
                                            <TableCell className="text-right">
                                                <Button
                                                    variant="destructive"
                                                    size="sm"
                                                    onClick={() => handleStop(c.cacheId)}
                                                    disabled={busy}
                                                    data-testid={`near-stop-${c.cacheId}`}
                                                >
                                                    <CircleStopIcon/>Stop
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                ) : (
                                    <TableRow data-testid="near-warm-empty-row">
                                        <TableCell colSpan={3} className="h-24 text-center text-muted-foreground">
                                            {loading ? "Loading…" : "No caches are currently warm."}
                                        </TableCell>
                                    </TableRow>
                                )}
                            </TableBody>
                        </Table>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
