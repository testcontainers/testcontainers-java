#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
#set -o xtrace

TASKS=()

# Core
TASKS+=("testcontainers:check")

# All modules
while read -r MODULE_DIRECTORY; do
    MODULE=$(basename "$MODULE_DIRECTORY")
    TASKS+=("${MODULE}:check")
done < <(find modules -type d -mindepth 1 -maxdepth 1)

# Examples
TASKS+=("-p examples check")

# Docs examples
TASKS+=("docs:examples:junit4:generic:check")
TASKS+=("docs:examples:junit4:redis:check")
TASKS+=("docs:examples:junit5:redis:check")
TASKS+=("docs:examples:spock:redis:check")

# Emit a JSON array of all tasks
JOINED=$(printf ", '%s'" "${TASKS[@]}")
echo "[${JOINED:1} ]"
