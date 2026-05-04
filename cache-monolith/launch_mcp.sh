#!/bin/bash

# Ensure we are in the cache-monolith directory or that the script finds the correct paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

ENV_FILE="$SCRIPT_DIR/aeron-cache-ui.env"
OPENAPI_SPEC="$ROOT_DIR/cache-http/openapi.yml"

if [ "$1" = "config" ] || [ "$1" = "--config" ]; then
  cat >&2 <<EOF
To use this MCP server in VS Code
add the following to your MCP settings file

{
  "servers": {
    "aeron-cache-monolith": {
      "type": "stdio",
      "command": "$SCRIPT_DIR/launch_mcp.sh",
      "args": []
    }
  },
  "inputs": []
}
EOF
  exit 0
fi

# Wait for the monolith to generate the env file with the dynamic ports
if [ ! -f "$ENV_FILE" ]; then
  echo "Waiting for $ENV_FILE to be generated..."
  while [ ! -f "$ENV_FILE" ]; do
    sleep 1
  done
fi

# Load the environment variables from the file
source "$ENV_FILE"

if [ -z "$AERON_CACHE_API" ]; then
  echo "Error: AERON_CACHE_API not found in $ENV_FILE"
  exit 1
fi

echo "Connecting MCP Server to $AERON_CACHE_API"

# Launch the OpenAPI MCP Server using the dynamically discovered base URL.
# This assumes Node.js and npx are available on the system.
exec npx -y mcp-openapi --spec "$OPENAPI_SPEC" --base-url "$AERON_CACHE_API"
