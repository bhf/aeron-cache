"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";
import {RemoveCacheItem} from "@/app/@cacheview/cache/[slug]/RemoveCacheItem";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";


export function getCacheItemColumns(cacheId: number): ColumnDef<CacheInfo>[] {
    return [
        {
            id: "remove",
            header: "Remove",
            cell: ({row}) => {
                const itemKey = row.getValue("key")
                const itemValue = row.getValue("value")

                return (
                    <div>
                        <RemoveCacheItem cacheId={cacheId} itemKey={itemKey as string}></RemoveCacheItem>asd
                    </div>
                )
            },
        },
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
            cell: ({row}) => {
                const itemKey = row.getValue("key")
                const itemValue = row.getValue("value")

                return (
                    <div>
                        <RemoveCacheItem cacheId={cacheId} itemKey={itemKey as string}></RemoveCacheItem>asd
                        <CopyToClipboard value={itemValue as string} tooltip={"Copy Value"} element={"Value"}></CopyToClipboard>
                        <CopyToClipboard value={itemKey as string} tooltip={"Copy Key"} element={"Value"}></CopyToClipboard>
                    </div>
                )
            },
        },
    ]
}

export const cacheItemColumns: ColumnDef<CacheInfo>[] = [
    {
        id: "remove",
        header: "Remove",
        cell: ({row}) => {
            const itemKey = row.getValue("key")
            const itemValue = row.getValue("value")
            const cacheId = row.getValue("cacheId")
            return (
                <div>
                    <RemoveCacheItem cacheId={cacheId as number} itemKey={itemKey as string}></RemoveCacheItem>
                </div>
            )
        },
    },
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
        id: "copy",
        header: "Copy",
        cell: ({row}) => {
            const itemKey = row.getValue("key")
            const itemValue = row.getValue("value")

            return (
                <div className={"flex flex-row space-x-2 mb-2"}>
                    <CopyToClipboard value={itemKey as string} tooltip={"Copy Key"} element={"Key"}></CopyToClipboard>
                    <CopyToClipboard value={itemValue as string} tooltip={"Copy Value"}
                                     element={"Value"}></CopyToClipboard>

                </div>
            )
        },
    },
]
