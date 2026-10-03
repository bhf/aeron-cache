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
import {useResponsivePageSize} from "@/hooks/useResponsivePageSize"
import * as React from "react"

import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from "@/components/ui/table"

interface DataTableProps<TData, TValue> {
    columns: ColumnDef<TData, TValue>[]
    data: TData[]
}

/**
 * A table for displaying items from within a cache.
 * @param columns The columns of the table.
 * @param data The data of the table.
 * @constructor
 */
export function CacheItemsDataTable<TData, TValue>({
                                             columns,
                                             data
                                         }: DataTableProps<TData, TValue>) {

    const [sorting, setSorting] = React.useState<SortingState>([])

    const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>(
        []
    )

    const [columnVisibility] = React.useState({'cacheId': false,});

    // Scale the number of visible rows to fill the viewport so large screens
    // aren't left mostly empty. 9 is the floor used on smaller screens. A
    // slightly smaller rowHeight than the default packs in ~10% more rows.
    const {ref: tableRef, pageSize} = useResponsivePageSize({min: 9, rowHeight: 48})

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
            columnVisibility: columnVisibility
        },
    })

    React.useEffect(() => {
        table.setPageSize(pageSize)
    }, [table, pageSize])

    return (
        <div>
            <div className="rounded-md border" ref={tableRef}>
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
                                    data-testid={`cache-item-row-${row.getValue("key")}`}
                                >
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
                                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={columns.length} className="h-24 text-center">
                                    No results.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
            <div className="flex items-center justify-between py-4">
                <Input
                    placeholder="Filter keys..."
                    value={(table.getColumn("key")?.getFilterValue() as string) ?? ""}
                    onChange={(event) =>
                        table.getColumn("key")?.setFilterValue(event.target.value)
                    }
                    className="max-w-sm"
                    data-testid="cache-items-filter-input"
                />
                <div className="flex items-center space-x-2">
                    <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => table.previousPage()}
                        disabled={!table.getCanPreviousPage()}
                    >
                        Previous
                    </Button>
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => table.nextPage()}
                        disabled={!table.getCanNextPage()}
                    >
                        Next
                    </Button>
                </div>
            </div>
        </div>
    )
}
