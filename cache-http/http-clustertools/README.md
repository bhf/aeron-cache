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