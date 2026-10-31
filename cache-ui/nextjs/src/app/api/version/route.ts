import {getLogger} from "@/lib/loggingUtil";

const logger = getLogger("VersionRoute")

export const dynamic = 'force-dynamic'

// The running version is injected at build/launch time so it matches the GitHub release tags
// (0.0.x-SNAPSHOT): the Docker image bakes it in via the APP_VERSION build arg (publish-ghcr.yml),
// and the Homebrew distribution exports it from the packaged .app-version file (brew-runner.sh).
// Unset for local `npm run dev`, where it falls back to "dev".
const CURRENT_VERSION = process.env.APP_VERSION ?? "dev"

const GITHUB_RELEASES_URL = "https://api.github.com/repos/bhf/aeron-cache/releases/latest"

// Cache the upstream lookup so repeated About dialog opens don't hammer the GitHub API, which
// rate-limits unauthenticated callers to 60 requests/hour per IP.
let cachedLatest: { version: string | null; fetchedAt: number } | null = null
const CACHE_TTL_MS = 10 * 60 * 1000 // 10 minutes

async function getLatestVersion(): Promise<string | null> {
    const now = Date.now()
    if (cachedLatest && now - cachedLatest.fetchedAt < CACHE_TTL_MS) {
        return cachedLatest.version
    }

    try {
        const res = await fetch(GITHUB_RELEASES_URL, {
            headers: {'Accept': 'application/vnd.github+json'},
            cache: "no-store",
        })
        if (!res.ok) {
            logger.warn("GitHub releases lookup returned " + res.status)
            return cachedLatest?.version ?? null
        }
        const data = await res.json()
        const tag: string = data.tag_name ?? ""
        const version = tag.startsWith("v") ? tag.slice(1) : tag
        cachedLatest = {version: version || null, fetchedAt: now}
        return cachedLatest.version
    } catch (err) {
        logger.warn("Error whilst fetching latest release", err)
        return cachedLatest?.version ?? null
    }
}

/**
 * Parse the numeric X.Y.Z prefix of a version, discarding any suffix such as "-SNAPSHOT".
 * Returns null when there is no numeric prefix to compare (e.g. the "dev" fallback).
 */
function parseVersion(v: string): [number, number, number] | null {
    const core = v.split("-")[0]
    const parts = core.split(".").map(Number)
    if (parts.length === 0 || parts.some(Number.isNaN)) return null
    return [parts[0] ?? 0, parts[1] ?? 0, parts[2] ?? 0]
}

function isNewer(latest: string, current: string): boolean {
    const l = parseVersion(latest)
    const c = parseVersion(current)
    if (!l || !c) return false
    for (let i = 0; i < 3; i++) {
        if (l[i] !== c[i]) return l[i] > c[i]
    }
    return false
}

/**
 * Report the running UI version alongside the latest published GitHub release, so the About dialog
 * can surface when a newer version is available. latestVersion is null when GitHub is unreachable.
 */
export async function GET() {
    const latestVersion = await getLatestVersion()
    const updateAvailable = latestVersion !== null && isNewer(latestVersion, CURRENT_VERSION)

    return Response.json({
        version: CURRENT_VERSION,
        latestVersion,
        updateAvailable,
    })
}
