import Link from "next/link";
import MenuLinks from "@/components/MenuLinks";

/**
 * The dark top navigation bar shared by every page: the Aeron Cache name and the menu buttons.
 */
export default function SiteHeader() {
    return (
        <nav className="site-header sticky top-0 z-40 border-b border-white/10 bg-[#0b0f0d]/95 backdrop-blur-md">
            <div className="flex items-center justify-between gap-4 px-5 py-3">
                <Link href={"/"} className="group whitespace-nowrap">
                    <span
                        className="text-sm font-light uppercase tracking-[0.25em] text-[#f4f6f4] transition group-hover:text-[#b8d8c0]">
                        Aeron&nbsp;Cache
                    </span>
                </Link>
                <MenuLinks/>
            </div>
        </nav>
    );
}
