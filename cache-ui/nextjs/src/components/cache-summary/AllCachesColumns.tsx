"use client"

import {ColumnDef} from "@tanstack/react-table"
import Link from "next/link"
import {CacheInfo} from "@/lib/types";
import {SearchCodeIcon} from "lucide-react";
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

            return (
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
            )
        },
    },
]
