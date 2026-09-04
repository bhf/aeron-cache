"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CounterItem} from "@/lib/types";
import {RemoveCounterItem} from "@/app/@cacheview/counters/[slug]/RemoveCounterItem";
import {CounterRowActions} from "@/app/@cacheview/counters/[slug]/CounterRowActions";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";

const MAX_CHARACTERS_MD_PLUS = 37
const MAX_CHARACTERS_SM_PLUS = 20

export const counterItemColumns: ColumnDef<CounterItem>[] = [
    {
        id: "remove",
        header: "Remove",
        cell: ({row}) => {
            const itemKey = row.getValue("key")
            const cacheId = row.getValue("cacheId")
            return (
                <div>
                    <RemoveCounterItem cacheId={cacheId as number} itemKey={itemKey as string}></RemoveCounterItem>
                </div>
            )
        },
    },
    {
        accessorKey: "key",
        header: "Key",
        cell: ({row}) => {
            const itemKey = row.getValue("key") as string
            const formattedKeyMD = itemKey.length > MAX_CHARACTERS_MD_PLUS ?
                itemKey.substring(0, MAX_CHARACTERS_MD_PLUS) + "....." : itemKey
            const formattedKeySM = itemKey.length > MAX_CHARACTERS_SM_PLUS ?
                itemKey.substring(0, MAX_CHARACTERS_SM_PLUS) + "....." : itemKey

            return (
                <div className={"mb-2"}>
                    <div className={"max-md:hidden"}>{formattedKeyMD}</div>
                    <div className={"min-md:hidden"}>{formattedKeySM}</div>
                </div>
            )
        }
    },
    {
        accessorKey: "value",
        header: "Value",
        cell: ({row}) => {
            const itemValue = row.getValue("value") as number
            return (
                <div className={"mb-2 font-mono"} data-testid={`counter-value-${row.getValue("key")}`}>
                    {itemValue}
                </div>
            )
        },
    },
    {
        id: "operations",
        header: "Operations",
        cell: ({row}) => {
            const itemKey = row.getValue("key") as string
            const cacheId = row.getValue("cacheId") as number
            return (
                <CounterRowActions cacheId={cacheId} itemKey={itemKey}/>
            )
        }
    },
    {
        id: "actions",
        header: "",
        cell: ({row}) => {
            const itemKey = row.getValue("key") as string
            const itemValue = row.getValue("value") as number
            return (
                <div className={"flex flex-row space-x-2 mb-1"}>
                    <CopyToClipboard value={itemKey} tooltip={"Copy Key"}
                                     element={"Key"}></CopyToClipboard>
                    <CopyToClipboard value={String(itemValue)} tooltip={"Copy Value"}
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
