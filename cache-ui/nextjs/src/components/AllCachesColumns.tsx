"use client"

import { ColumnDef } from "@tanstack/react-table"
import Link from "next/link"
import {CacheInfo} from "@/lib/types";

export const liveUsersCols: ColumnDef<CacheInfo>[] = [
    {
        accessorKey: "cacheId",
        header: "CacheId",
    },
    {
        accessorKey: "itemCount",
        header: "Item Count",
    },
    /*{
        id: "actions-session-flow",
        header: "Session Flow",
        cell: ({ row }) => {
            const sessionId = row.getValue("sessionId")
            const userSessions = "/admin/session/"+sessionId

            return (
                <Link href={userSessions}>
                    View Session
                </Link>
            )
        },
    },*/
]
