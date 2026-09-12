# Cache WS

Websocket services for subscribing to instances of Aeron Cache.

## API specs

- [`ws-openapi.yaml`](ws-openapi.yaml) — one-directional cache subscription routes (subscribe via the connect URL).
- [`counters-ws-openapi.yaml`](counters-ws-openapi.yaml) — one-directional counter subscription routes.
- [`bidi-ws-openapi.yaml`](bidi-ws-openapi.yaml) — the bidirectional endpoint (`/api/ws/v1/bidi`) carrying the full
  cache/counter command surface plus dynamic subscribe/unsubscribe, correlated per request.