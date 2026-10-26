import type {Metadata} from "next";
import "../globals.css";
import {Toaster} from "@/components/ui/sonner";

/**
 * Root layout for full-page views that are not part of the cache browser's parallel route layout, such
 * as the observability dashboard opened in its own tab.
 */
export const metadata: Metadata = {
    title: "Aeron Cache",
    description: "Aeron Cache",
};

export default function StandaloneLayout({children}: Readonly<{ children: React.ReactNode }>) {
    return (
        <html lang="en">
        <body>
        {children}
        <Toaster/>
        </body>
        </html>
    );
}
