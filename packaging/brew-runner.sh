#!/bin/bash
set -e

# This wrapper is used by the Homebrew distribution.
# Setup paths based on the package layout.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/cache-monolith"
FRONTEND_DIR="$SCRIPT_DIR/cache-ui/nextjs"

CONFIG_DIR="$HOME/.aeron-cache"
mkdir -p "$CONFIG_DIR"

# Generate a default configuration file if the user hasn't provided one
USER_ENV_FILE="$CONFIG_DIR/backend.env"
if [ ! -f "$USER_ENV_FILE" ]; then
    echo "Creating default backend config at $USER_ENV_FILE..."
    touch "$USER_ENV_FILE"
fi

UI_ENV_FILE="$CONFIG_DIR/aeron-cache-ui.env"
# Remove old generator UI env
rm -f "$UI_ENV_FILE"

echo "🚀 Starting Aeron Cache Monolith backend..."
cd "$BACKEND_DIR"
# Pass the user env config, then intercept the UI config output
# Main.java generates aeron-cache-ui.env in the CWD, which is now $BACKEND_DIR
"$BACKEND_DIR/run_with_env.sh" "$USER_ENV_FILE" &
BACKEND_PID=$!

echo "⏳ Waiting for UI environment file to be generated at $BACKEND_DIR/aeron-cache-ui.env..."
while [ ! -f "$BACKEND_DIR/aeron-cache-ui.env" ]; do
  sleep 1
done

# Copy it to our user's config dir so they can inspect it and the frontend can use it
cp "$BACKEND_DIR/aeron-cache-ui.env" "$UI_ENV_FILE"

echo "🚀 Starting Aeron Cache UI frontend..."
# Export variables for node server
set -a
source "$UI_ENV_FILE"
set +a

# Find a free port starting from 3000
PORT=3000
while (echo >/dev/tcp/localhost/$PORT) >/dev/null 2>&1; do
    echo "⚠️ Port $PORT is in use, trying next..."
    PORT=$((PORT + 1))
done

echo "Starting Next.js standalone server on port $PORT..."
cd "$FRONTEND_DIR"
PORT=$PORT node server.js &
FRONTEND_PID=$!

echo
echo "✅ Aeron Cache is running!"
echo "   UI      : http://localhost:$PORT"
echo "   Backend : PID $BACKEND_PID"
echo "   Config  : $CONFIG_DIR"
echo 
echo "Press Ctrl+C to stop."

# Wait indefinitely, handling graceful shutdown
cleanup() {
    echo "🛑 Shutting down backend and frontend..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    rm -f "$BACKEND_DIR/aeron-cache-ui.env"
}
trap cleanup SIGINT SIGTERM EXIT

wait
