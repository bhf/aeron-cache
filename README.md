![img.png](docs/images/header.png)

# Aeron Cache

A clustered cache built using Aeron Cluster for RAFT.

## How To Run

### Application
1. Run ClusterLauncher to spin up a 3 node cluster.
2. Run SampleClientUsage.

### Docker (wip)

Cache cluster example:
```
cd cache-cluster
docker build --tag 'aeron-cache'
docker run 'aeron-cache'
```

Sample cache client usage:
```
cd cache-client
docker build --tag 'aeron-client'
docker run 'aeron-client'
```

### JUnit Tests

```bash
./gradlew test
```

### JMH Tests

```bash
./gradlew jmh
```

### Overview

#### Message Flow Overview

![img_1.png](docs/images/msgFlow.png)

#### Cluster Service Workflow

![img.png](docs/images/cluster-flow.png)

## Future Work

* Cache keys and values to be SBE encoded/decoded
* Cluster side queries via serializable consumers
* Annotation processor
* Startup and periodic snapshot handling 
* Off heap cache implementation
* Activation and passivation strategies
* Custom key entropy source

