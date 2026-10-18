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
read-only, and returns them as JSON. The driver directory is taken from the `AERON_DIR` environment
variable, falling back to the Aeron default when unset. A 503 is returned when the CnC file cannot be
read (for example because the driver is not running).

```azure
curl http://localhost:7080/api/v1/clustertools/counters
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