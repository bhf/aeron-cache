# http-fanout

HTTP scatter-gather aggregator for a **multi-cache** deployment.

Several independent cache clusters each sit behind their own `http-server-javalin` gateway. The
caches are **partitioned** — a given `cacheId` lives on exactly one cluster. This module fronts all
of those gateways with a single HTTP interface that **scatters each admin/observability request to
every downstream gateway and gathers the responses back into one aggregated result with the same
JSON shape** the gateways already serve, so the UI and existing scripts get a cluster-wide view
without change.

It is a **pure HTTP aggregator**: no Aeron cluster connection, no near cache — it only makes outbound
HTTP calls to the configured gateways (see `GatewayFanoutClient`) and merges their JSON
(see `Aggregators`).

## Configuration (environment)

| Var | Default | Meaning |
|---|---|---|
| `GATEWAY_ADDRESSES` | (empty) | Comma-separated gateway base URLs, e.g. `http://aeroncache-http-a:7070,http://aeroncache-http-b:7070` |
| `HTTP_PORT` | `7071` | Port the aggregator listens on |
| `CORS_ALLOWED_ORIGINS` | localhost dev origins | Allowed CORS origins |
| `REQUEST_TIMEOUT_MS` | `5000` | Per-gateway request timeout |

## Aggregated endpoints

| Method | Path | Merge (partitioned topology) |
|---|---|---|
| GET | `/api/v1/caches` | union of every gateway's caches |
| GET | `/api/v1/counters-caches` | union |
| GET | `/api/v1/stats` | field-wise sum |
| GET | `/api/v1/counters-stats` | field-wise sum |
| GET | `/api/v1/timers` | union of pending timers |
| GET | `/api/v1/snapshot-info` | representative (first present) |
| GET | `/api/v1/aeron-counters` | every gateway's nodes flattened |
| POST | `/api/v1/snapshot` | trigger on every cluster; OK iff all succeed |
| POST | `/api/v1/snapshot-and-purge` | aggregate per-node purge results |
| POST | `/api/v1/shutdown` | shut down every cluster; OK iff all succeed |

Plus `/liveness/`, `/readiness/` and `/prometheus` (the aggregator's own metrics).

A gateway that is unreachable or errors is excluded from sums / surfaced as an unreachable
contributor rather than failing the whole request.

## Build

```
./gradlew :cache-http:http-fanout:build          # compile + unit tests
./gradlew :cache-http:http-fanout:jibDockerBuild  # build image aeroncache-http-fanout:latest
```
