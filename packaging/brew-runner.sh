#!/bin/bash
set -e

# This wrapper is used by the Homebrew distribution.
# Resolve symlink to find actual install path (in Homebrew Cellar)
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Homebrew installs the actual files into 'libexec' and places this script in 'bin'
BACKEND_DIR="$SCRIPT_DIR/../libexec/cache-monolith"
FRONTEND_DIR="$SCRIPT_DIR/../libexec/cache-ui/nextjs"

CONFIG_DIR="$HOME/.aeron-cache"
mkdir -p "$CONFIG_DIR"

PID_FILE="$CONFIG_DIR/runner.pid"
PORT_FILE="$CONFIG_DIR/ui_port"
UI_ENV_FILE="$CONFIG_DIR/aeron-cache-ui.env"
LOG_FILE="$CONFIG_DIR/aeron-cache.log"

print_banner() {
cat << "EOF"
    ___    __________  ____  _   __   _________   ________  _________
   /   |  / ____/ __ \/ __ \/ | / /  / ____/   | / ____/ / / / ____/
  / /| | / __/ / /_/ / / / /  |/ /  / /   / /| |/ /   / /_/ / __/   
 / ___ |/ /___/ _, _/ /_/ / /|  /  / /___/ ___ / /___/ __  / /___   
/_/  |_/_____/_/ |_|\____/_/ |_/   \____/_/  |_\____/_/ /_/_____/   

   https://github.com/bhf/aeron-cache
EOF
}

echo "Initializing Aeron Cache..."
print_banner

if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
    echo "⚡ Aeron Cache is already running (PID $(cat "$PID_FILE"))."
    if [ -f "$PORT_FILE" ]; then
        echo "   UI Link            : http://localhost:$(cat "$PORT_FILE")"
    fi
    if [ -f "$UI_ENV_FILE" ]; then
        echo "   Backend Endpoints  :"
        grep -v '^#' "$UI_ENV_FILE" | grep -v '^[[:space:]]*$' | sed 's/^/      /'
    fi
    echo ""
    exit 0
fi

echo $$ > "$PID_FILE"

# Generate a default configuration file if the user hasn't provided one
USER_ENV_FILE="$CONFIG_DIR/backend.env"
if [ ! -f "$USER_ENV_FILE" ]; then
    echo "Creating default backend config at $USER_ENV_FILE..."
    cat << 'EOF' > "$USER_ENV_FILE"
CLUSTER_NODE=0
CLUSTER_ADDRESSES=localhost
EGRESS_IP=localhost
POD_ADDRESS=aeron-cache-http

// Config for single node monolith cache
HTTP_RESPONSE_PUB_HOST=localhost
WS_RESPONSE_PUB_HOST=localhost
SSE_RESPONSE_PUB_HOST=localhost

// Config for single node clients like http and ws interfaces
REQUEST_PUB_HOST=localhost

// Cache mode
CACHE_MODE=RAFT

LAUNCH_EMBEDDED=false
AERON_DIR=aeron
CLUSTER_FOLDER=node0/cluster/
EOF
fi

# Remove old generator UI env and log
rm -f "$UI_ENV_FILE"
rm -f "$LOG_FILE"

echo "🚀 Starting Aeron Cache Monolith backend..." >> "$LOG_FILE"
cd "$BACKEND_DIR"
# Pass the user env config, then intercept the UI config output
# Main.java generates aeron-cache-ui.env in the CWD, which is now $BACKEND_DIR
"$BACKEND_DIR/run_with_env.sh" "$USER_ENV_FILE" "$@" >> "$LOG_FILE" 2>&1 &
BACKEND_PID=$!

echo "⏳ Waiting for UI environment file to be generated at $BACKEND_DIR/aeron-cache-ui.env..." >> "$LOG_FILE"
while [ ! -f "$BACKEND_DIR/aeron-cache-ui.env" ]; do
  sleep 1
done

# Copy it to our user's config dir so they can inspect it and the frontend can use it
cp "$BACKEND_DIR/aeron-cache-ui.env" "$UI_ENV_FILE"

echo "🚀 Starting Aeron Cache UI frontend..." >> "$LOG_FILE"
# Export variables for node server
set -a
source "$UI_ENV_FILE"
set +a

# Find a free port starting from 3000
PORT=3000
while (echo >/dev/tcp/localhost/$PORT) >/dev/null 2>&1; do
    echo "⚠️ Port $PORT is in use, trying next..." >> "$LOG_FILE"
    PORT=$((PORT + 1))
done

echo $PORT > "$PORT_FILE"

echo "Starting Aeron Cache UI on port $PORT..." >> "$LOG_FILE"
cd "$FRONTEND_DIR"
PORT=$PORT node server.js >> "$LOG_FILE" 2>&1 &
FRONTEND_PID=$!

echo
echo "✅ Aeron Cache is running!"
echo "   UI      : http://localhost:$PORT"
echo "   Backend : PID $BACKEND_PID"
echo "   Config  : $CONFIG_DIR"
echo "   Log     : $LOG_FILE"
echo 
echo "Press Ctrl+C to stop."

# Wait indefinitely, handling graceful shutdown
cleanup() {
    echo "🛑 Shutting down backend and frontend..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    rm -f "$BACKEND_DIR/aeron-cache-ui.env"
    rm -f "$PID_FILE" "$PORT_FILE"
}
trap cleanup SIGINT SIGTERM EXIT

wait
