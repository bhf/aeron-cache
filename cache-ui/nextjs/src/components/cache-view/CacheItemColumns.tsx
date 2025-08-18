"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";
import {RemoveCacheItem} from "@/app/@cacheview/cache/[slug]/RemoveCacheItem";
import CopyToClipboard from "@/components/cache-view/CopyToClipboard";

const MAX_CHARACTERS_MD_PLUS = 37
const MAX_CHARACTERS_SM_PLUS = 20

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
            const formattedKeyMD = itemKey.length > MAX_CHARACTERS_MD_PLUS ?
                itemKey.substring(0, MAX_CHARACTERS_MD_PLUS) + "....." : itemKey
            const formattedKeySM = itemKey.length > MAX_CHARACTERS_SM_PLUS ?
                itemKey.substring(0, MAX_CHARACTERS_SM_PLUS) + "....." : itemKey

            return (
                <div className={"flex flex-row space-x-2 mb-2"}>
                    <div className={"max-md:hidden"}>{formattedKeyMD}</div>
                    <div className={"min-md:hidden"}>{formattedKeySM}</div>
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
            const formattedValueMD = itemValue.length > MAX_CHARACTERS_MD_PLUS ?
                itemValue.substring(0, MAX_CHARACTERS_MD_PLUS) + "....." : itemValue
            const formattedValueSM = itemValue.length > MAX_CHARACTERS_MD_PLUS ?
                itemValue.substring(0, MAX_CHARACTERS_SM_PLUS) + "....." : itemValue

            return (
                <div className={"flex flex-row space-x-2 mb-2"}>
                    <div className={"max-md:hidden"}>{formattedValueMD}</div>
                    <div className={"min-md:hidden"}>{formattedValueSM}</div>
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
