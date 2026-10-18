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