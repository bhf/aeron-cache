# aeron-media-driver

The Aeron **C media driver** (`aeronmd`), packaged to run as a per-pod sidecar so
the Java apps can attach as pure Aeron clients instead of each launching an
embedded Java `MediaDriver`.

## Build

```sh
# From the repo root (extracts the Aeron version from gradle/libs.versions.toml):
make build-media-driver

# Or directly:
docker build docker/aeron-media-driver \
  -t aeroncache-media-driver:latest \
  --build-arg AERON_VERSION=1.50.0
```

The driver is compiled from the `real-logic/aeron` source at the pinned tag. That
version **must** match `io.aeron:aeron-all` (`gradle/libs.versions.toml` → `aeron`)
— the client and driver share the CnC file layout and are only compatible within a
matching semantic version.

## Runtime configuration

`aeronmd` reads `AERON_*` environment variables directly. The entrypoint fills in
sidecar defaults for anything unset:

| Env var | Default | Notes |
|---|---|---|
| `AERON_DIR` | `/dev/shm/aeron` | Shared with the app container via the `shm` emptyDir. |
| `AERON_THREADING_MODE` | `SHARED` | Matches the current embedded-driver mode. |
| `AERON_DIR_DELETE_ON_START` | `true` | Driver owns dir lifecycle; cleans stale state on restart. |
| `AERON_DIR_DELETE_ON_SHUTDOWN` | `false` | Let a crashed app reconnect to a live driver. |
| `AERON_TERM_BUFFER_SPARSE_FILE` | `true` | Keeps large (128m) terms mostly virtual in `/dev/shm`. |
| `AERON_TERM_BUFFER_LENGTH` / `AERON_IPC_TERM_BUFFER_LENGTH` | from `AERON_CACHE_TERM_LENGTH` | Single source of truth shared with the Java apps. |
| `AERON_PRINT_CONFIGURATION` | `true` | Dumps effective config to the container log on start. |

## k8s wiring

Deployed as a **native sidecar** (an `initContainers` entry with
`restartPolicy: Always`) so the driver is up before the app and shuts down last.
The driver and app containers must share the `shm` volume **and** a common
`runAsUser` / `fsGroup` so both can open the CnC + term files. Wiring the sidecar
into the interface charts (behind an `externalMediaDriver.enabled` toggle) is
Phase 1.
