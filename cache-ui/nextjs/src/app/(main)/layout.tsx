import type {Metadata} from "next";
import "../globals.css";
import {Toaster} from "@/components/ui/sonner";
import SiteHeader from "@/components/SiteHeader";


export const metadata: Metadata = {
    title: "Aeron Cache",
    description: "Aeron Cache",
};

export default function RootLayout({
                                       children, dashboard, main, cacheview
                                   }: Readonly<{
    children: React.ReactNode,
    dashboard: React.ReactNode,
    main: React.ReactNode,
    cacheview: React.ReactNode,
}>) {

    return (
        <html lang="en">
        <body>
        <SiteHeader/>
        <div className="grid grid-cols-1 md:grid-cols-2 h-full min-h-screen">
            <div className="pl-2 pt-3">
                {dashboard}
                {main}
            </div>
            <div className="pl-2 pt-3">
                {cacheview}
            </div>
        </div>
        <Toaster/>
        </body>
        </html>
    );
}
