#!/usr/bin/env bash
# Posts N items into a cache via the HTTP API.
# Usage: put-items.sh [count] [cacheId] [baseUrl]
set -euo pipefail

COUNT="${1:-100}"
CACHE_ID="${2:-test}"
BASE_URL="${3:-http://localhost:7070/api/v1}"

[[ "$COUNT" =~ ^[0-9]+$ ]] || { echo "count must be a non-negative integer" >&2; exit 1; }

# Create the cache first (ignore failure if it already exists)
curl -s -o /dev/null -X POST "$BASE_URL/cache" \
  -H 'Content-Type: application/json' \
  -d "{\"cacheId\":\"$CACHE_ID\"}" || true

failed=0
for ((i = 1; i <= COUNT; i++)); do
  status=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/cache/$CACHE_ID" \
    -H 'Content-Type: application/json' \
    -d "{\"key\":\"key-$i\",\"value\":\"value-$i\"}")
  if [[ "$status" != "200" ]]; then
    echo "item $i failed with HTTP $status" >&2
    failed=$((failed + 1))
  fi
done

echo "Posted $((COUNT - failed))/$COUNT items to cache '$CACHE_ID'"
[[ $failed -eq 0 ]]
