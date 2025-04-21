"use client"

import {ColumnDef} from "@tanstack/react-table"
import Link from "next/link"
import {CacheInfo} from "@/lib/types";
import {BoltIcon, SearchCodeIcon, ZapIcon} from "lucide-react";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger,} from "@/components/ui/tooltip"

export const allCachesColumns: ColumnDef<CacheInfo>[] = [
    {
        accessorKey: "cacheId",
        header: "CacheId",
    },
    {
        accessorKey: "itemCount",
        header: "Item Count",
    },
    {
        id: "actions",
        header: "",
        cell: ({row}) => {
            const cacheId = row.getValue("cacheId")
            const editCache = "/cache/" + cacheId
            const cacheWs = "/wss/" + cacheId

            return (
                <div className={"flex space-x-2"}>
                    <Link href={editCache}>
                        <TooltipProvider>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <SearchCodeIcon size={20}/>
                                </TooltipTrigger>
                                <TooltipContent>
                                    <p>Cache Details</p>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>
                    </Link>

                    <Link href={cacheWs}>
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
                </div>
            )
        },
    },
]
