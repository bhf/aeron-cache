import type {Metadata} from "next";
import "./globals.css";
import {Radio} from "@deemlol/next-icons";
import Link from "next/link";
import {Toaster} from "@/components/ui/sonner";
import MenuLinks from "@/components/MenuLinks";


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

    function header() {
        return (
            <div>
                <div
                    className="header absolute top-0 w-full space-x-5 flex justify-left sticky pb-2 pt-2 pl-2 bg-linear-to-t from-gray-100 to-gray-200">

                    <Link href={"/"}>
                        <div className="text-3xl text-gray-900 dark:text-white">
                            <div className={"pl-6"}>
                                <Radio size={38} color="#37912f"/>
                            </div>
                            Aeron Cache
                        </div>
                    </Link>
                    <MenuLinks/>
                </div>
            </div>
        );
    }

    return (
        <html lang="en">
        <body>
        {header()}
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
