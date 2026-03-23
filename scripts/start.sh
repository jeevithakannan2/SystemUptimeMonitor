#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "── Starting all services ──"
docker-compose up --build "$@"
