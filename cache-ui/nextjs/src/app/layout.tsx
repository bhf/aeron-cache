import type {Metadata} from "next";
import "./globals.css";
import {Github, Linkedin, Star} from "@deemlol/next-icons";


export const metadata: Metadata = {
    title: "Aeron Cache",
    description: "Aeron Cache",
};

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="en">
        <body antialiased="true">
        {children}
        <div className="footer absolute bottom-0 w-full space-x-15 flex justify-center items-center">
            <div className="space-x-2 flex justify-center items-center">
            <a href="https://github.com/bhf/aeron-cache" target={"_blank"}>Github</a><Github size={20} color="black" />
            </div>
            <div className="space-x-2 flex justify-center items-center">
                <a href="https://www.linkedin.com/in/sanjeevsarda/" target={"_blank"}>LinkedIn</a><Linkedin size={20} color="black" />
            </div>
            <div className="space-x-2 flex justify-center items-center">
                <a href="https://sanjeev.pages.dev/" target={"_blank"}>StayTuned</a><Star size={20} color="black" />
            </div>
        </div>
        </body>
        </html>
    );
}
