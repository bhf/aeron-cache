#!/bin/sh
# Entrypoint for the Aeron C media driver sidecar.
#
# aeronmd reads its configuration straight from AERON_* environment variables, so
# this script only fills in sensible k8s-sidecar defaults for anything the pod spec
# did not set, then execs the driver as PID 1 so it receives SIGTERM directly.
set -eu

# Fixed sidecar defaults (a Helm value can still override any of these via env).
: "${AERON_DIR:=/dev/shm/aeron}"
: "${AERON_THREADING_MODE:=SHARED}"
: "${AERON_DIR_DELETE_ON_START:=true}"
: "${AERON_DIR_DELETE_ON_SHUTDOWN:=false}"
: "${AERON_TERM_BUFFER_SPARSE_FILE:=true}"
: "${AERON_PRINT_CONFIGURATION:=true}"

# Single source of truth for term length: the Java apps are configured with
# AERON_CACHE_TERM_LENGTH. Mirror it onto the driver's publication + IPC term
# buffers unless those were set explicitly. aeronmd accepts k/m/g suffixes.
if [ -n "${AERON_CACHE_TERM_LENGTH:-}" ]; then
    : "${AERON_TERM_BUFFER_LENGTH:=${AERON_CACHE_TERM_LENGTH}}"
    : "${AERON_IPC_TERM_BUFFER_LENGTH:=${AERON_CACHE_TERM_LENGTH}}"
fi

export AERON_DIR AERON_THREADING_MODE AERON_DIR_DELETE_ON_START \
    AERON_DIR_DELETE_ON_SHUTDOWN AERON_TERM_BUFFER_SPARSE_FILE AERON_PRINT_CONFIGURATION
[ -n "${AERON_TERM_BUFFER_LENGTH:-}" ] && export AERON_TERM_BUFFER_LENGTH
[ -n "${AERON_IPC_TERM_BUFFER_LENGTH:-}" ] && export AERON_IPC_TERM_BUFFER_LENGTH

echo "aeron-media-driver: dir=${AERON_DIR} threading=${AERON_THREADING_MODE}" \
     "term=${AERON_TERM_BUFFER_LENGTH:-<default>} ipcTerm=${AERON_IPC_TERM_BUFFER_LENGTH:-<default>}" \
     "deleteOnStart=${AERON_DIR_DELETE_ON_START}"

exec /opt/aeron/aeronmd
