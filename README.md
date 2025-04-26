![img.png](docs/images/header.png)

# Aeron Cache

A clustered cache built using Aeron Cluster for RAFT.

### How To Run

```bash
git clone https://github.com/bhf/aeron-cache
cd aeron-cache/
./gradlew build
docker compose build
docker compose up
```



### Structure

__cache-client__ - An Aeron cluster based client for the cache.

__cache-cluster__ - The core cache cluster service.

__cache-common__ - Common entities and classes used in cache implementations.

__cache-http__ - REST interfaces around the cache-client. Various implementations including Javalin.

__cache-messages-sbe__ - Core SBE messages used by the cache.

__cache-messages-http__ - Messages used by HTTP interfaces to the cache.

__cache-ui__ - A UI that uses the REST API provided by cache-http-server

__k8s__ - Helm charts and other K8s resources

__hyperfoil__ - Some basic hyperfoil tests



### Overview

#### Message Flow Overview

![img_1.png](docs/images/msgFlow2.png)

#### Cluster Service Workflow

![img.png](docs/images/cluster-flow2.png)



## Future Work

https://sanjdev.atlassian.net/jira/software/projects/AC/boards/22