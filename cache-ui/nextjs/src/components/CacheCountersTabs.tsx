"use client"

import React, {useState} from "react";
import {Button} from "@/components/ui/button";

interface CacheCountersTabsProps {
    caches: React.ReactNode
    counters: React.ReactNode
}

/**
 * A lightweight tab switcher for the main panel that toggles between
 * the caches and counters views. Both panels stay mounted (hidden via
 * CSS) so their async content and any streaming state is preserved when
 * switching tabs.
 * @constructor
 */
export default function CacheCountersTabs(props: CacheCountersTabsProps) {

    const [tab, setTab] = useState<"caches" | "counters">("caches")

    return (
        <div>
            <div className="flex gap-2 px-6 pt-4 pb-2">
                <Button
                    variant={tab === "caches" ? "default" : "outline"}
                    onClick={() => setTab("caches")}
                    data-testid="caches-tab"
                >
                    Caches
                </Button>
                <Button
                    variant={tab === "counters" ? "default" : "outline"}
                    onClick={() => setTab("counters")}
                    data-testid="counters-tab"
                >
                    Counters
                </Button>
            </div>
            <div className={tab === "caches" ? "" : "hidden"} data-testid="caches-tab-panel">
                {props.caches}
            </div>
            <div className={tab === "counters" ? "" : "hidden"} data-testid="counters-tab-panel">
                {props.counters}
            </div>
        </div>
    );
}
