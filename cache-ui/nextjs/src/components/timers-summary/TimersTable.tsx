"use client"

import {
    ColumnDef,
    ColumnFiltersState,
    flexRender,
    getCoreRowModel,
    getFilteredRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    SortingState,
    useReactTable,
} from "@tanstack/react-table"

import {Button} from "@/components/ui/button"
import {Input} from "@/components/ui/input"
import {useRouter} from "next/navigation"
import * as React from "react"

import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from "@/components/ui/table"

interface DataTableProps<TData, TValue> {
    columns: ColumnDef<TData, TValue>[]
    data: TData[]
}

// Small grace period after a deadline before re-fetching, to give the backend
// time to fire the timer and drop the item from getAllTimers.
const REFRESH_GRACE_MS = 1500

/**
 * A table for displaying pending TTL removal timers across caches and
 * counter caches, with a per-row cancel action.
 * @param columns Columns of the table.
 * @param data The data of the table.
 * @constructor
 */
export function TimersDataTable<TData, TValue>({
                                                   columns,
                                                   data
                                               }: DataTableProps<TData, TValue>) {

    const router = useRouter()

    const [sorting, setSorting] = React.useState<SortingState>([
        {id: "deadline", desc: false},
    ])

    const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>(
        []
    )

    // The backend removes an item (and its timer) when the TTL fires, but does
    // not push that removal to us. Schedule a refresh for the moment the next
    // timer becomes due so expired rows drop off on their own. If a row is
    // already past due (backend not yet caught up), poll again shortly until it
    // clears. Re-runs whenever the data changes, so each refresh re-arms for the
    // next deadline.
    React.useEffect(() => {
        const deadlines = (data as unknown as { deadline?: number }[])
            .map((d) => d?.deadline)
            .filter((d): d is number => typeof d === "number")

        if (deadlines.length === 0) {
            return
        }

        const now = Date.now()
        const soonestDeadline = Math.min(...deadlines)
        const delay = Math.max(REFRESH_GRACE_MS, soonestDeadline - now + REFRESH_GRACE_MS)

        const timeout = setTimeout(() => router.refresh(), delay)
        return () => clearTimeout(timeout)
    }, [data, router])

    const table = useReactTable({
        data,
        columns,
        getCoreRowModel: getCoreRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        onSortingChange: setSorting,
        getSortedRowModel: getSortedRowModel(),
        onColumnFiltersChange: setColumnFilters,
        getFilteredRowModel: getFilteredRowModel(),
        state: {
            sorting,
            columnFilters,
        },
        initialState: {
            pagination: {
                pageSize: 5, //custom default page size
            },
        },
    })

    return (
        <div>
            <div className="rounded-md border" data-testid="all-timers-table">
                <Table>
                    <TableHeader>
                        {table.getHeaderGroups().map((headerGroup) => (
                            <TableRow key={headerGroup.id}>
                                {headerGroup.headers.map((header) => {
                                    return (
                                        <TableHead key={header.id}>
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                    header.column.columnDef.header,
                                                    header.getContext()
                                                )}
                                        </TableHead>
                                    )
                                })}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows?.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow
                                    key={row.id}
                                    data-state={row.getIsSelected() && "selected"}
                                    data-testid={`timer-row-${row.id}`}
                                >
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
                                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            <TableRow data-testid="all-timers-empty-row">
                                <TableCell colSpan={columns.length} className="h-24 text-center">
                                    No pending timers.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
            <div className="flex items-center justify-between py-4">
                <Input
                    placeholder="Filter by cache..."
                    value={(table.getColumn("cacheId")?.getFilterValue() as string) ?? ""}
                    onChange={(event) =>
                        table.getColumn("cacheId")?.setFilterValue(event.target.value)
                    }
                    className="max-w-sm"
                    data-testid="all-timers-filter-input"
                />
                <div className="flex items-center space-x-2">
                    <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => table.previousPage()}
                        disabled={!table.getCanPreviousPage()}
                        data-testid="all-timers-prev-page"
                    >
                        Previous
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.nextPage()}
                        disabled={!table.getCanNextPage()}
                        data-testid="all-timers-next-page"
                    >
                        Next
                    </Button>
                </div>
            </div>
        </div>
    )
}
