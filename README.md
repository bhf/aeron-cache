# Aeron Cache

A clustered cache built using Aeron Cluster and inspired by Infinispan and Flink.

## How To Run

1. Run ClusterLauncher to spin up a 3 node cluster.
2. Run SampleClientUsage.

## Future Work

* Cache keys and values to be SBE encoded
* Cluster side queries via serializable consumers
* Annotation processor
* JPMS integration
* Off heap cache implementation
* Activation and passivation strategies
* Custom key entropy source

