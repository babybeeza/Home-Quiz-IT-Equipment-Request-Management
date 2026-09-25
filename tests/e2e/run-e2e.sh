#!/usr/bin/env bash
# Runs the Playwright acceptance suite in Docker against a locally started stack (TASK-008).
#   Compose PostgreSQL/Redis → seed dataset → backend jar (:8080) → frontend production build (:3200) → Playwright.
# The browser runs inside the Playwright container, so the app is addressed as host.docker.internal.
# Usage (repository root, Docker running): tests/e2e/run-e2e.sh [extra playwright args]
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
ROOT_W=$(cd "$ROOT" && pwd -W 2>/dev/null || pwd)
FRONT_PORT=${E2E_FRONTEND_PORT:-3200}
HOST_URL=http://host.docker.internal
PLAYWRIGHT_IMAGE=mcr.microsoft.com/playwright:v1.63.0-noble
JAR="$ROOT/backend/target/equipment-request-service-0.0.1-SNAPSHOT.jar"
LOGS="$ROOT/tests/e2e/results"
mkdir -p "$LOGS"

cleanup() {
  docker rm -f e2e-frontend > /dev/null 2>&1 || true
  powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force }" > /dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[e2e] database and seed"
docker compose -f "$ROOT/compose.yaml" up -d --wait postgres redis > /dev/null
docker compose -f "$ROOT/compose.yaml" exec -T postgres psql -q -U equipment_app -d equipment_requests -v ON_ERROR_STOP=1 \
  < "$ROOT/tests/performance/seed-search-dataset.sql" > /dev/null

[ -f "$JAR" ] || (cd "$ROOT/backend" && ./mvnw -q -DskipTests package)
echo "[e2e] backend :8080 (CORS origin $HOST_URL:$FRONT_PORT)"
FRONTEND_ORIGIN="$HOST_URL:$FRONT_PORT" java -jar "$JAR" > "$LOGS/backend.log" 2>&1 &
for _ in $(seq 1 90); do curl -sf http://localhost:8080/actuator/health/liveness > /dev/null 2>&1 && break; sleep 1; done

echo "[e2e] frontend production build :$FRONT_PORT"
MSYS_NO_PATHCONV=1 docker run -d --name e2e-frontend -p "$FRONT_PORT:3000" \
  -e NEXT_PUBLIC_API_BASE_URL="$HOST_URL:8080/api/v1" \
  -v "$ROOT_W:/workspace" -v /workspace/frontend/node_modules -v /workspace/frontend/.next \
  -w /workspace/frontend node:24.15.0-alpine \
  sh -c "npm ci --no-audit --no-fund > /dev/null && npm run build > /dev/null && npm start -- -p 3000" > /dev/null
for _ in $(seq 1 300); do curl -sf "http://localhost:$FRONT_PORT/requests" > /dev/null 2>&1 && break; sleep 1; done
curl -sf "http://localhost:$FRONT_PORT/requests" > /dev/null || { docker logs e2e-frontend | tail -20; exit 1; }

echo "[e2e] playwright"
set +e
MSYS_NO_PATHCONV=1 docker run --rm --ipc=host \
  -e E2E_BASE_URL="$HOST_URL:$FRONT_PORT" -e E2E_API_URL="$HOST_URL:8080/api/v1" \
  -v "$ROOT_W/tests/e2e:/e2e" -v /e2e/node_modules -w /e2e "$PLAYWRIGHT_IMAGE" \
  sh -c "npm ci --no-audit --no-fund > /dev/null && npx playwright test $*"
STATUS=$?
set -e
echo "[e2e] playwright exit $STATUS (report: tests/e2e/playwright-report/index.html)"
exit $STATUS
