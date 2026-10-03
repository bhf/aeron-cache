"use client"

import * as React from "react"

interface ResponsivePageSizeOptions {
    /** Minimum number of rows to show (also the SSR / pre-measurement default). */
    min: number
    /** Approximate rendered height, in px, of a single table body row. */
    rowHeight?: number
    /** Approximate height, in px, of the table header row. */
    headerHeight?: number
    /**
     * Space, in px, to leave below the table for the pagination controls, card
     * padding and a little breathing room at the bottom of the viewport.
     */
    footerHeight?: number
}

/**
 * Computes a table page size that fills the available vertical space below the
 * table down to the bottom of the viewport, so large/high-resolution screens
 * show more rows instead of leaving the page empty. Falls back to `min` on
 * small screens and before the first measurement (e.g. during SSR).
 *
 * Attach the returned `ref` to the element that sits at the top of the table
 * (the bordered table container). The page size recomputes on window resize.
 */
export function useResponsivePageSize<T extends HTMLElement = HTMLDivElement>(
    {min, rowHeight = 53, headerHeight = 45, footerHeight = 96}: ResponsivePageSizeOptions
): { ref: React.RefObject<T | null>, pageSize: number } {
    const ref = React.useRef<T>(null)
    const [pageSize, setPageSize] = React.useState(min)

    React.useEffect(() => {
        function compute() {
            const el = ref.current
            if (!el) {
                return
            }
            const top = el.getBoundingClientRect().top
            const available = window.innerHeight - top - headerHeight - footerHeight
            const rows = Math.floor(available / rowHeight)
            setPageSize(Math.max(min, rows))
        }

        compute()
        window.addEventListener("resize", compute)
        return () => window.removeEventListener("resize", compute)
    }, [min, rowHeight, headerHeight, footerHeight])

    return {ref, pageSize}
}
