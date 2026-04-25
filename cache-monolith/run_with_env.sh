#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <path_to_env_file> [additional_java_args...]"
  exit 1
fi

ENV_FILE="$1"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: $ENV_FILE not found!"
  exit 1
fi

# Read the env file, ignoring empty lines and lines starting with //
while IFS='=' read -r key value; do
  # Check if it has a key and doesn't start with //
  if [[ -n "$key" && ! "$key" =~ ^[[:space:]]*// ]]; then
    # Remove carriage returns in case of CRLF line endings
    key=$(echo "$key" | tr -d '\r')
    value=$(echo "$value" | tr -d '\r')
    export "$key=$value"
  fi
done < "$ENV_FILE"

shift # Remove the env file from the arguments list

# Dynamically locate the jar relative to the script location
# Handles Gradle distributions (bin/ and lib/) and local dev (cache-monolith/build/libs/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH=$(find "$SCRIPT_DIR/build/libs" "$SCRIPT_DIR/../lib" "$SCRIPT_DIR/../libexec" "$SCRIPT_DIR/../build/libs" -maxdepth 1 \( -name "*cache-monolith*-all.jar" -o -name "*cache-monolith*.jar" \) 2>/dev/null | grep -v -e "javadoc" -e "sources" -e "plain" | head -n 1)

if [ -z "$JAR_PATH" ]; then
  echo "Error: Could not find the application jar file."
  exit 1
fi

JAVA_OPTS="--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED"

# Execute the application Jar, forwarding any remaining arguments
exec java $JAVA_OPTS -jar "$JAR_PATH" "$@"
