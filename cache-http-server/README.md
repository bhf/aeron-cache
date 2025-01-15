# Cache HTTP Server

A HTTP server exposing cache functions over REST.

## Endpoints

### Create Cache: 

POST localhost:7070/api/v1/cache/

Body:

{cacheId}

### Put Item in Cache:

POST localhost:7070/api/v1/cache/{cacheId}/

Body:

Key, Value

### Get Item from Cache:

GET localhost:7070/api/v1/cache/{cacheId}/{key}

### Remove Item from Cache:

DELETE localhost:7070/api/v1/cache/{cacheId}/{key}

### Delete Cache

DELETE localhost:7070/api/v1/cache/{cacheId}/

