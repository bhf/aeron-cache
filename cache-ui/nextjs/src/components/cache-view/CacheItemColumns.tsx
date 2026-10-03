"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";
import {RemoveCacheItem} from "@/app/@cacheview/cache/[slug]/RemoveCacheItem";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";
import TruncatedText from "@/components/cache-view/TruncatedText";
import JsonViewer from "@/components/cache-view/JsonViewer";

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
            return (
                <div className={"mb-2"}>
                    <TruncatedText text={itemKey}/>
                </div>
            )
        }
    },
    {
        accessorKey: "value",
        header: "Value",
        cell: ({row}) => {
            const itemValue = row.getValue("value") as string
            return (
                <div className={"mb-2 flex items-center gap-2"}>
                    <TruncatedText text={itemValue}/>
                    <JsonViewer value={itemValue}/>
                </div>
            )
        },
    },
    {
        id: "actions",
        header: "Actions",
        cell: ({row}) => {
            const itemKey = row.getValue("key") as string
            const itemValue = row.getValue("value") as string
            return (
                <div className={"flex flex-row space-x-2 mb-1"}>
                    <CopyToClipboard value={itemKey as string} tooltip={"Copy Key"}
                                     element={"Key"}></CopyToClipboard>
                    <CopyToClipboard value={itemValue as string} tooltip={"Copy Value"}
                                     element={"Value"}></CopyToClipboard>
                </div>
            )
        }
    },
    {
        accessorKey: "cacheId",
        header: "CacheId",
        enableHiding: true,
    },
]
