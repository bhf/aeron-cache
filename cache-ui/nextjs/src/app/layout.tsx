import type {Metadata} from "next";
import "./globals.css";
import {Github, Linkedin, Radio, Star} from "@deemlol/next-icons";
import Link from "next/link";


export const metadata: Metadata = {
    title: "Aeron Cache",
    description: "Aeron Cache",
};

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode;
}>) {

    function footer() {
        return <div className="footer absolute bottom-0 w-full space-x-15 flex justify-center items-center pb-1 pt-1 bg-linear-to-t from-gray-200 to-gray-100">
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
        return <div className="header absolute top-0 w-full space-x-5 flex justify-left sticky pb-2 pt-2 pl-2 bg-linear-to-t from-gray-100 to-gray-200">
            
            <Link href={"/"}>
            <div className="text-3xl text-gray-900 dark:text-white">
                <div className={"pl-6"}>
                <Radio size={38} color="#37912f" />
                </div>
                Aeron Cache
            </div>
            </Link>
        </div>;
    }

    return (
        <html lang="en">
        <body>
        {header()}
        <div className="pl-2 pt-3">
        {children}
        </div>
        {footer()}
        </body>
        </html>
    );
}
