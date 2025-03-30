import type {Metadata} from "next";
import "./globals.css";


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
            <a href="https://github.com/bhf/aeron-cache" target={"_blank"}>Github</a>
            <a href="https://www.linkedin.com/in/sanjeevsarda/" target={"_blank"}>LinkedIn</a>
            <a href="https://sanjeev.pages.dev/" target={"_blank"}>Blog</a>
        </div>
        </body>
        </html>
    );
}
