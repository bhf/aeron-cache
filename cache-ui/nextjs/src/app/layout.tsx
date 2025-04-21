import type {Metadata} from "next";
import "./globals.css";
import {Radio} from "@deemlol/next-icons";
import Link from "next/link";
import {Toaster} from "@/components/ui/sonner";
import AboutAeronCache from "@/components/About";
import {LayersIcon, TrelloIcon} from "lucide-react";
import {Button} from "@/components/ui/button";


export const metadata: Metadata = {
    title: "Aeron Cache",
    description: "Aeron Cache",
};

export default function RootLayout({
                                       children, dashboard, main
                                   }: Readonly<{
    children: React.ReactNode,
    dashboard: React.ReactNode,
    main: React.ReactNode
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
                    <div className="hidden lg:flex lg:flex-1 lg:justify-end pr-4">
                        <AboutAeronCache/>
                        <Link href={"http://localhost:16686/"} target="_blank"><Button
                            variant="link"><LayersIcon/>Jaeger</Button></Link>
                        <Link href={"http://localhost:9090/"} target="_blank"><Button
                            variant="link"><TrelloIcon/>Prometheus</Button></Link>
                    </div>
                </div>

            </div>
        );
    }

    return (
        <html lang="en">
        <body>
        {header()}
        <div className="pl-2 pt-3">
            {dashboard}
            {main}
        </div>
        <Toaster/>
        </body>
        </html>
    );
}
