#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "── Building frontend ──"
cd frontend
npm install --silent
npm run build -- --outDir dist
cd ..

echo "── Building WAR ──"
./mvnw clean package -DskipTests -q

echo "── Starting services ──"
docker compose up --build "$@"
