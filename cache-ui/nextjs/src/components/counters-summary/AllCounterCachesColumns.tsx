"use client";

import {ColumnDef} from "@tanstack/react-table";
import Link from "next/link";
import {CounterCacheInfo} from "@/lib/types";
import {SearchCodeIcon, SendIcon, ZapIcon} from "lucide-react";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger,} from "@/components/ui/tooltip";

export const allCounterCachesColumns: ColumnDef<CounterCacheInfo>[] = [
    {
        accessorKey: "cacheId",
        header: "Counter Cache ID",
    },
    {
        accessorKey: "itemCount",
        header: "Counter Count",
    },
    {
        id: "actions",
        header: "",
        cell: ({row}) => {
            const cacheId = row.getValue("cacheId");
            const viewCounters = "/counters/" + cacheId;
            const countersWs = "/counters-wss/" + cacheId;
            const countersSse = "/counters-sse/" + cacheId;

            return (
                <div className={"flex space-x-2"}>
                    <Link href={viewCounters} data-testid={`view-counter-cache-${cacheId}`}>
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <SearchCodeIcon size={20}/>
                                </TooltipTrigger>
                                <TooltipContent>
                                    <p>Counter Cache Details</p>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </Link>

                    <Link href={countersWs}>
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <ZapIcon size={20}/>
                                </TooltipTrigger>
                                <TooltipContent>
                                    <p>Websockets</p>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </Link>

                    <Link href={countersSse}>
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <SendIcon size={20}/>
                                </TooltipTrigger>
                                <TooltipContent>
                                    <p>SSE</p>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </Link>
                </div>
            );
        },
    },
];
