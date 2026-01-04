# Near Cache Implementation

## http-server-near-javalin

A near cache implementation for GET operations.

Currently, this will subscribe to the cache and locally cache items on all keys, not just the ones you do GET operations on.
Also supports creating caches - if you create a cache using the ```near``` endpoints, it will be created as a regular Aeron Cache (remotely) with the
client setting up the subscription and local copy.