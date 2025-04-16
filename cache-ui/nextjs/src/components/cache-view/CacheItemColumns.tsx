"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";
import {RemoveCacheItem} from "@/app/@main/cache/[slug]/RemoveCacheItem";



export function getCacheItemColumns(cacheId: number):ColumnDef<CacheInfo>[] {
    return [
        {
            accessorKey: "key",
            header: "Key",
        },
        {
            accessorKey: "value",
            header: "Value",
        },
        {
            id: "actions",
            header: "Actions",
            cell: ({ row }) => {
                const itemKey = row.getValue("key")

                return (
                    <RemoveCacheItem cacheId={cacheId} itemKey={itemKey as string}></RemoveCacheItem>
                )
            },
        },
    ]
}

export const cacheItemColumns: ColumnDef<CacheInfo>[] = [
    {
        accessorKey: "key",
        header: "Key",
    },
    {
        accessorKey: "value",
        header: "Value",
    },
    {
        accessorKey: "cacheId",
        header: "CacheId",
        enableHiding: true,

    },
    {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => {
            const itemKey = row.getValue("key")
            const cacheId = row.getValue("cacheId")

            return (
                    <RemoveCacheItem cacheId={cacheId as number} itemKey={itemKey as string}></RemoveCacheItem>
            )
        },
    },
]
