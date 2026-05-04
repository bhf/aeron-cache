#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${1:-$ROOT_DIR/config/singlenode-dev.env}"

echo "🚀 Starting cache-monolith backend..."
"$SCRIPT_DIR/run_with_env.sh" "$ENV_FILE" &
BACKEND_PID=$!

UI_ENV_FILE="$SCRIPT_DIR/aeron-cache-ui.env"
echo "⏳ Waiting for UI environment file to be generated at $UI_ENV_FILE..."
while [ ! -f "$UI_ENV_FILE" ]; do
  sleep 1
done

echo "🚀 Starting cache-ui frontend..."
cd "$ROOT_DIR/cache-ui/nextjs"

# Copy the generated env file so Next.js picks it up
cp "$UI_ENV_FILE" .env.local

if [ ! -d "node_modules" ]; then
    echo "📦 Installing Next.js dependencies..."
    npm install
fi

echo "📦 Building Next.js frontend..."
npm run build

# Find a free port starting from 3000
PORT=3000
while (echo >/dev/tcp/localhost/$PORT) >/dev/null 2>&1; do
    echo "⚠️ Port $PORT is in use, trying next..."
    PORT=$((PORT + 1))
done

echo "🚀 Starting cache-ui frontend on port $PORT..."
PORT=$PORT npm start &
FRONTEND_PID=$!

# Clean up children on exit
cleanup() {
    echo "🛑 Shutting down..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    if [ -f "$UI_ENV_FILE" ]; then
        echo "🧹 Cleaning up generated UI env file..."
        rm "$UI_ENV_FILE"
    fi
}
trap cleanup SIGINT SIGTERM EXIT

wait
