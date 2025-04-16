import type {Metadata} from "next";
import "./globals.css";
import {ExternalLink, Github, Linkedin, Radio, Star} from "@deemlol/next-icons";
import Link from "next/link";
import {Toaster} from "@/components/ui/sonner";


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

    function footer() {
        return <div
            className="footer mt-auto absolute bottom-0 w-full space-x-15 flex justify-center items-center pb-1 pt-1 bg-linear-to-t from-gray-200 to-gray-100">
            <div className="space-x-2 flex justify-center items-center">
                <a href="https://github.com/bhf/aeron-cache" target={"_blank"}>Github</a>
                <Github size={20} color="black"/>
            </div>
            <div className="space-x-2 flex justify-center items-center">
                <a href="https://www.linkedin.com/in/sanjeevsarda/" target={"_blank"}>LinkedIn</a>
                <Linkedin size={20} color="black"/>
            </div>
            <div className="space-x-2 flex justify-center items-center">
                <a href="https://sanjeev.pages.dev/" target={"_blank"}>StayTuned</a>
                <Star size={20} color="black"/>
            </div>
        </div>;
    }

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
                        <Link href="https://github.com/bhf/aeron-cache/tree/main/docs" target={"_blank"}
                              className={"pr-1"}>
                            Help
                        </Link>
                        <ExternalLink size={15} color="black"/>

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
