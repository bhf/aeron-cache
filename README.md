![img.png](docs/images/header.png)

# Aeron Cache

*__Hand crafted without LLMs or Agents.__*

A clustered cache built using Aeron, Agrona and SBE. UI with NextJS, Shadcn and Tailwind. Includes HTTP and websocket interfaces with support for multi-cache joins over websocket. Prometheus+cAdvisor and tracing with Jaeger+OTEL.

## How To Run

### Docker
```bash
git clone https://github.com/bhf/aeron-cache
cd aeron-cache/
./gradlew build
docker compose build
docker compose up
```

* Frontend on localhost:3000
* HTTP API on localhost:7070
* Websocket on localhost:7071
* Jaeger tracing on localhost:16686
* Prometheus on localhost:9090
* cAdvisor on localhost:8080

### Helm and K8s

You can see example k8s config in folders called "k8s" in application modules.
In ```/k8s/helm/``` there are some Helm charts which are a work in progress.

## Subscribe to Multiple Caches over Websocket

To subscribe to cache updates on caches with IDs 808 and 333:

```bash
uwsc http://localhost:7071/api/ws/v1/caches/808,333
```

## UI

![img.png](cache-ui/ui-main-page.png)

![img.png](cache-ui/ui-cache-view.png)

![img.png](cache-ui/ui-cache-ws.png)

## Structure

__cache-client__ - An Aeron cluster based client for the cache.

__cache-cluster__ - The core cache cluster service.

__cache-common__ - Common entities and classes used in cache implementations.

__cache-http__ - REST interfaces around the cache-client.

__cache-ws__ - Websocket interfaces around the cache-client.

__cache-messages-sbe__ - Core SBE messages used by the cache.

__cache-messages-http__ - Messages used by HTTP interfaces to the cache.

__cache-ui__ - A UI that uses the REST API provided by cache-http-server

__k8s__ - Helm charts and other K8s resources (work in progress)

__hyperfoil__ - Some basic hyperfoil tests



## Overview

### Message Flow Overview

![img_1.png](docs/images/msgFlow2.png)

### Cluster Service Workflow

![img.png](docs/images/cluster-flow2.png)



## Future Work

* Persisting and loading from an Image
* Abstraction for configurable Map implementation (off heap maps)
* Non-clustered mode (single node cache)
* Industrialization and cache-ops

https://sanjdev.atlassian.net/jira/software/projects/AC/boards/22