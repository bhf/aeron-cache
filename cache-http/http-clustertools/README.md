# HTTP ClusterTools

A HTTP wrapper around Aeron ClusterTools.

## Snapshotting

```azure
curl -X POST http://localhost:7080/api/v1/clustertools -H "Content-Type: application/json" -d '{
              "tool": "snapshot",
              "clusterFolder": "[REPLACE WITH YOUR PATH]/aeron-cache/node0/cluster/"
             }'

```

## Shutdown

```azure
curl -X POST http://localhost:7080/api/v1/clustertools -H "Content-Type: application/json" -d '{
              "tool": "shutdown",
              "clusterFolder": "[REPLACE WITH YOUR PATH]/aeron-cache/node0/cluster/"
             }'

```

## Purge and snapshot + purge

Purge old cluster log recording segments (reclaiming disk) without taking a new snapshot:

```azure
curl -X POST http://localhost:7080/api/v1/clustertools -H "Content-Type: application/json" -d '{
              "tool": "purge",
              "clusterFolder": "[REPLACE WITH YOUR PATH]/aeron-cache/node0/cluster/"
             }'
```

Take a snapshot, wait for it to become durable, then purge — the combined operation:

```azure
curl -X POST http://localhost:7080/api/v1/clustertools -H "Content-Type: application/json" -d '{
              "tool": "snapshot-and-purge",
              "clusterFolder": "[REPLACE WITH YOUR PATH]/aeron-cache/node0/cluster/"
             }'
```

The purge never removes log segments newer than the Nth most recent snapshot (`SNAPSHOT_RETENTION_COUNT`, default 2), leaving room for follower catch-up. The latest purge result is surfaced on `snapshot-info` as `lastPurge`.

## Scheduled snapshot + purge

The sidecar can also run `snapshot-and-purge` on a timer in-process. It is **disabled by default**
(`SNAPSHOT_SCHEDULE_ENABLED=false`) and requires `CLUSTER_FOLDER` to be set. This only affects the
scheduled path — the endpoints above always run regardless of any check.

Before each scheduled run a **check** decides whether the run is worthwhile, so snapshots track real
change instead of a fixed clock. The check also owns the polling rate, so a cheap default can poll
rarely while a change-aware check polls often and only occasionally triggers the (expensive) snapshot.
Pick the strategy with `SNAPSHOT_CHECK_STRATEGY`:

| Strategy | Behaviour | Key tunables |
| --- | --- | --- |
| `ALWAYS` (default) | Always snapshot on a fixed interval — the original behaviour. | `SNAPSHOT_INTERVAL` (ISO-8601, default `PT6H`) |
| `CACHE_STATE_CHANGE` | Snapshot once enough **state-changing** cache operations have accrued since the last snapshot, read from the custom request counters (type `2000`) in the media driver. Read-only ops are excluded. | `SNAPSHOT_CHECK_POLL_INTERVAL` (default `PT5M`), `SNAPSHOT_CHECK_STATE_CHANGE_THRESHOLD` (default `10000`) |
| `RECORDING_GROWTH` | Snapshot once the archive has grown by enough bytes since the last purge. | `SNAPSHOT_CHECK_POLL_INTERVAL` (default `PT5M`), `SNAPSHOT_CHECK_ARCHIVE_GROWTH_BYTES` (default `268435456` = 256 MiB) |

The `CACHE_STATE_CHANGE` strategy relies on the media driver counters, so `AERON_DIR` must point at the
node's driver (see below). All of these are exposed in the Helm charts (`aeroncache-cluster`'s
`clustertools.snapshot`, `aeroncache-monolith`'s and `aeroncache-http-clustertools`'s `snapshot`) and in
the local/brew `*.env` files, disabled by default so nothing changes until opted in.

Two Testcontainers soak workflows exercise the scheduled path end to end — confirming the check holds
off while idle and fires once its threshold is crossed, without calling any endpoint directly:
`scheduled-snapshot-state-change.yml` (light) and `scheduled-snapshot-recording-growth.yml` (heavy).

## Media driver counters

Scrapes the co-located Aeron media driver's own counters (publications, subscriptions, flow
control, errors, etc. — not the cache counters) by memory-mapping the driver's `cnc.dat` file
read-only, and returns them as JSON. A 503 is returned when the CnC file cannot be read (for example
because the driver is not running).

The driver directory is taken from the `AERON_DIR` environment variable, falling back to the Aeron
default when unset. It can be overridden per request with a `?dir=` query parameter. This matters
because the directory differs by deployment mode:

- **External media driver** (`externalMediaDriver.enabled=true`): the node and driver share a fixed
  dir, e.g. `/dev/shm/aeron`.
- **Embedded media driver** (default): the node derives its dir as
  `/dev/shm/aeron-<user>-<nodeId>-driver`, e.g. `/dev/shm/aeron-root-0-driver` for node 0 as root.

The `aeroncache-cluster` Helm chart sets `AERON_DIR` on the clustertools sidecar to the correct value
for both modes, so the default endpoint works out of the box.

```azure
# Use the configured AERON_DIR
curl http://localhost:7080/api/v1/clustertools/counters

# Point at a specific driver directory (e.g. an embedded driver)
curl 'http://localhost:7080/api/v1/clustertools/counters?dir=/dev/shm/aeron-root-0-driver'
```

```json
{
  "aeronDirectoryName": "/dev/shm/aeron",
  "cncVersion": 16,
  "timestamp": 1760000000000,
  "count": 42,
  "counters": [
    { "counterId": 0, "typeId": 0, "value": 123, "label": "Bytes sent" }
  ]
}
```

### Prometheus exposition

The same media driver counters are also exported in Prometheus text format on `/prometheus`, appended
after the clustertools JVM metrics, so a single Prometheus target per pod captures everything the
driver publishes. If the driver cannot be read the JVM metrics are still served and the Aeron counters
are simply omitted from that scrape (the target does not flap).

```bash
curl http://localhost:7080/prometheus
```

Three metric families are produced:

- `aeron_counter{counter_id,type_id,label}` — every allocated driver counter, verbatim. This is the
  complete, lossless export (media driver system counters, publications, subscriptions, plus the cache
  and gateway application counters). It powers the general *Aeron Cache - Transport / Media Driver*
  Grafana dashboard.
- `aeron_cache_requests_total{operation,node}` — the cache request counters (Aeron counter type
  `2000`, registered by `AeronCounterTracingService`), parsed into a clean per-operation metric for
  the *Cache Service* dashboard.
- `aeron_gateway_requests_total{type,node}`, `aeron_gateway_sessions_connected{node}`,
  `aeron_gateway_sessions_total{node}`, `aeron_gateway_stream_updates_published_total{node}` — the
  gateway counters (Aeron counter type `2100`, registered by `AeronCounterGatewayMetrics`), parsed for
  the *Transport Gateway* dashboard.

The parsed families are a convenience layer on top of `aeron_counter`: a counter whose label does not
match the expected structured pattern is still present in `aeron_counter`, so nothing is dropped.

The `aeroncache-cluster`, `aeroncache-ephemeral` and `aeroncache-gateway` charts annotate their pods
with `prometheus.io/scrape=true` (port `7080`, path `/prometheus`) so the `aeroncache-observability`
Prometheus auto-discovers and scrapes this sidecar. This works identically whichever cache flavour is
deployed.