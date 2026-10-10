# Aeron Cache
![img.png](docs/images/header.png)

[![Backend CI](https://github.com/bhf/aeron-cache/actions/workflows/ci.yaml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/ci.yaml)
[![Frontend CI](https://github.com/bhf/aeron-cache/actions/workflows/ui-ci.yaml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/ui-ci.yaml)
[![Helm CI](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci.yaml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci.yaml)
[![Helm CI Ephemeral](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci-ephemeral.yaml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci-ephemeral.yaml)
[![K8s CI Monolith Cache](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci-monolith.yaml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/helm-ci-monolith.yaml)
[![Microbenchmarks](https://github.com/bhf/aeron-cache/actions/workflows/microbenchmarks.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/microbenchmarks.yml)
[![Soak Core Cache](https://github.com/bhf/aeron-cache/actions/workflows/soak-core-cache.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/soak-core-cache.yml)
[![Soak Streaming](https://github.com/bhf/aeron-cache/actions/workflows/soak-streaming.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/soak-streaming.yml)
[![Soak Bidi WS](https://github.com/bhf/aeron-cache/actions/workflows/soak-bidi-ws.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/soak-bidi-ws.yml)
[![Soak Streaming WS](https://github.com/bhf/aeron-cache/actions/workflows/soak-streaming-ws.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/soak-streaming-ws.yml)
[![Soak SSE](https://github.com/bhf/aeron-cache/actions/workflows/soak-sse.yml/badge.svg)](https://github.com/bhf/aeron-cache/actions/workflows/soak-sse.yml)

A key value store with counters built using Aeron, Agrona and SBE. RAFT clustered or single node - fast by design. UI with NextJS, Shadcn and Tailwind. 
Includes HTTP, WS, SSE and Aeron transport (UDP and IPC) interfaces.

Deployable on Kubernetes with ```helm``` or locally with ```brew```.


* [Features](#features)
* [How To Run - Brew](#brew)
* [How To Run - K8s/Helm](#k8s-and-helm)
* [API Specs](#api-specs)



https://github.com/user-attachments/assets/c602f365-2b6a-497c-a671-29508cc04155


https://github.com/user-attachments/assets/cdbf0e54-2ff8-47c4-8a98-50a8104de6fd



## Features

* Key-value and counter caches with item level TTL support
* Bulk atomic operations across caches and counters
* Streaming multi-cache (and key) subscriptions
* PATCH support for writing partial updates to JSON values including partial streaming subscriptions (JSON Merge Patch RFC 7386)
* Near cache implementation (read ahead)

* HTTP (Request-Response), WS (UniDi and BiDi), SSE (UniDi) and SBE-Aeron (BiDi) APIs
* Embedded cache [polyglot clients](https://github.com/bhf/aeron-cache-embedded):
  * Aeron Transport (BIDI) - Java and Rust
  * HTTP and WS - Java, Rust, Typescript and Python
* Rust based [CLI](https://github.com/bhf/aeron-cache-cli)


## How To Run

### Brew
```bash
brew tap bhf/aeron-cache
brew install aeron-cache
aeron-cache
```

You should see something like:

```bash
Initializing Aeron Cache...

    ___    __________  ____  _   __   _________   ________  _________
   /   |  / ____/ __ \/ __ \/ | / /  / ____/   | / ____/ / / / ____/
  / /| | / __/ / /_/ / / / /  |/ /  / /   / /| |/ /   / /_/ / __/   
 / ___ |/ /___/ _, _/ /_/ / /|  /  / /___/ ___ / /___/ __  / /___   
/_/  |_/_____/_/ |_|\____/_/ |_/   \____/_/  |_\____/_/ /_/_____/   

              🌲 https://github.com/bhf/aeron-cache 🌲

✅ Aeron Cache is running!
   UI      : http://localhost:3000
   Backend : PID 6221
   Config  : /home/user1/.aeron-cache
   Log     : /home/user1/.aeron-cache/aeron-cache.log

Press Ctrl+C to stop.
```

  #### Configuring the monolith

The brew install runs the monolith (cluster node plus the interfaces below) and the UI. On first run a default config is
written to ```~/.aeron-cache/backend.env```

| Interface | Variable | Default |
|-----------|----------|---------|
| HTTP (REST) and cluster tools | always on | enabled |
| WebSocket gateway | ```WEBSOCKET_GATEWAY_ENABLED``` | ```true``` |
| SSE gateway | ```SSE_GATEWAY_ENABLED``` | ```true``` |
| Aeron (SBE) gateway | ```AERON_GATEWAY_ENABLED``` | ```false``` |

Other options:

| Variable | Default | Description |
|----------|---------|-------------|
| ```GATEWAY_TRANSPORT_MEDIA``` | ```udp``` | Aeron gateway transport, ```udp``` or ```ipc``` (only used when the Aeron gateway is enabled) |
| ```MONOLITH_EMBEDDED_DRIVER``` | ```true``` | ```true``` launches an embedded media driver; ```false``` attaches to an external ```aeronmd``` already running at ```AERON_DIR``` |
| ```DYNAMIC_CACHE_CREATION``` | ```false``` | Allow caches to be created dynamically |
| ```CACHE_MODE``` | ```RAFT``` | Cache mode (set in the generated config) |

For example, to enable the Aeron gateway over IPC and disable SSE, add to ```~/.aeron-cache/backend.env```:

```bash
AERON_GATEWAY_ENABLED=true
GATEWAY_TRANSPORT_MEDIA=ipc
SSE_GATEWAY_ENABLED=false
```

A disabled gateway is not started, so the matching UI streaming features (WebSocket/SSE) will not connect.


[Top](#aeron-cache)

### K8s and Helm

The ```Makefile``` is setup to push images to Minikube.

```bash
git clone https://github.com/bhf/aeron-cache
cd aeron-cache/
make all
cd k8s/helm/
make install-all
```

All targets below are run from ```k8s/helm/```. Each ```install-*``` target has a matching ```uninstall-*```.

```bash
make install-backend              # backend only
make install-frontend             # frontend (UI) only
make install-backend-ephemeral    # ephemeral backend only
make install-all-ephemeral        # ephemeral backend + UI
make uninstall-all                # tear down
```

Individual components can also be installed on their own, e.g. ```make install-http``` or ```make install-ui```.

#### External media driver

Pass ```EXTERNAL_MEDIA_DRIVER=true``` to any install target to run the Aeron C media driver (```aeronmd```) as a
sidecar. 

Build the driver image with ```make build-media-driver``` (part of ```make build-backend```).

```bash
make install-all EXTERNAL_MEDIA_DRIVER=true
make install-backend EXTERNAL_MEDIA_DRIVER=true
make install-backend-ephemeral EXTERNAL_MEDIA_DRIVER=true
```

#### Using images from GHCR

By default the charts use images built into Minikube. Pass ```GHCR=true``` to pull the released images from
```ghcr.io``` instead (no local build needed).

```bash
make install-all GHCR=true
make install-backend-ephemeral GHCR=true EXTERNAL_MEDIA_DRIVER=true
make install-all GHCR=true GHCR_TAG=1.2.3          # a specific release
```



![img.png](docs/images/k9s-screenshot.png)

[Top](#aeron-cache)

## API Specs

* [REST OpenAPI](cache-http/openapi.yml)
* [Streaming Websocket OpenAPI](cache-ws/ws-openapi.yaml)
* [Streaming SSE OpenAPI](cache-sse/sse-openapi.yaml)
* [BiDi Websocket OpenAPI](cache-ws/bidi-ws-openapi.yaml)
* [BiDi Aeron Transport Gateway SBE](cache-aeron-gateway/aeron-gateway-server/src/main/resources/sbe/gateway-schema.xml)


[Top](#aeron-cache)


