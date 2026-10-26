import type {Metadata} from "next";
import ObservabilityDashboard from "@/components/observability/ObservabilityDashboard";

export const metadata: Metadata = {
    title: "Aeron Cache - Observability",
};

/**
 * The observability dashboard as a full page, for keeping open in its own browser tab.
 */
export default function Page() {
    return (
        <div className="p-6 space-y-4">
            <h1 className="text-2xl">Observability</h1>
            <ObservabilityDashboard/>
        </div>
    )
}
