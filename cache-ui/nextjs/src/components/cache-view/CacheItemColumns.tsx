"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";
import {RemoveCacheItem} from "@/app/@cacheview/cache/[slug]/RemoveCacheItem";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";

function maxCharacters() {
    return 37;
}

export const cacheItemColumns: ColumnDef<CacheInfo>[] = [
    {
        id: "remove",
        header: "Remove",
        cell: ({row}) => {
            const itemKey = row.getValue("key")
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
        cell: ({row}) => {
            const itemKey = row.getValue("key") as string
            const cleaned = itemKey.length > maxCharacters() ? itemKey.substring(0, maxCharacters()) + "....." : itemKey
            return (
                <div className={"flex flex-row space-x-2 mb-2"}>
                    <div>{cleaned}</div>
                    <CopyToClipboard value={itemKey as string} tooltip={"Copy Key"}
                                     element={"Key"}></CopyToClipboard>

                </div>
            )
        }
    },
    {
        accessorKey: "value",
        header: "Value",
        cell: ({row}) => {
            const itemValue = row.getValue("value") as string
            const cleaned = itemValue.length > maxCharacters() ? itemValue.substring(0, maxCharacters()) + "....." : itemValue
            return (
                <div className={"flex flex-row space-x-2 mb-2"}>
                    <div>{cleaned}</div>
                    <CopyToClipboard value={itemValue as string} tooltip={"Copy Value"}
                                     element={"Value"}></CopyToClipboard>

                </div>
            )
        },
    },
    {
        accessorKey: "cacheId",
        header: "CacheId",
        enableHiding: true,
    },
]
