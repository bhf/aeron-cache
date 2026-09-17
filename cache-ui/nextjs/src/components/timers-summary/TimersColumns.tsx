"use client";

import {ColumnDef} from "@tanstack/react-table";
import {TimerInfo} from "@/lib/types";
import {Badge} from "@/components/ui/badge";
import {CancelTimer} from "@/components/timers-summary/CancelTimer";
import {TimeRemaining} from "@/components/timers-summary/TimeRemaining";

export const timersColumns: ColumnDef<TimerInfo>[] = [
    {
        accessorKey: "timerType",
        header: "Type",
        cell: ({row}) => {
            const timerType = row.getValue("timerType") as string
            return (
                <Badge variant={timerType === "COUNTER" ? "secondary" : "default"}>
                    {timerType}
                </Badge>
            )
        },
    },
    {
        accessorKey: "cacheId",
        header: "Cache ID",
    },
    {
        accessorKey: "key",
        header: "Key",
    },
    {
        accessorKey: "deadline",
        header: "Fires At",
        cell: ({row}) => {
            const deadline = row.getValue("deadline") as number
            return (
                <span>{new Date(deadline).toLocaleString()}</span>
            )
        },
    },
    {
        id: "remaining",
        header: "Time Remaining",
        cell: ({row}) => {
            const deadline = row.getValue("deadline") as number
            return (
                <TimeRemaining deadline={deadline}/>
            )
        },
    },
    {
        id: "actions",
        header: "",
        cell: ({row}) => {
            const timerType = row.getValue("timerType") as string
            const cacheId = row.getValue("cacheId") as string
            const key = row.getValue("key") as string
            return (
                <CancelTimer timerType={timerType} cacheId={cacheId} itemKey={key}/>
            )
        },
    },
];
