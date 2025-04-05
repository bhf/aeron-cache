"use client"

import { ColumnDef } from "@tanstack/react-table"
import Link from "next/link"
import {CacheInfo} from "@/lib/types";

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
        header: "Actions",
        cell: ({ row }) => {
            const cacheId = row.getValue("cacheId")
            const editCache = "/cache/"+cacheId

            return (
                <Link href={editCache}>
                    Details
                </Link>
            )
        },
    },
]
