#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

exec docker compose \
  --env-file "${BASE_DIR}/.env" \
  --env-file "${BASE_DIR}/release-state.env" \
  -f "${BASE_DIR}/docker-compose.yml" \
  "$@"
