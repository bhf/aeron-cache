"use client"

import {ColumnDef} from "@tanstack/react-table"
import {CacheInfo} from "@/lib/types";

export const cacheItemColumns: ColumnDef<CacheInfo>[] = [
    {
        accessorKey: "key",
        header: "Key",
    },
    {
        accessorKey: "value",
        header: "Value",
    },
]
