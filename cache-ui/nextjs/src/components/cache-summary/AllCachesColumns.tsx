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
]
