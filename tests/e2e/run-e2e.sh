#!/usr/bin/env bash
# Runs the Playwright acceptance suite against the Docker stack in compose.yaml (profile e2e, TASK-009).
# Uses its own Compose project (home-quiz-e2e) and ports, so it never touches the development
# database volume, and removes everything afterwards (KEEP_STACK=1 keeps it running for inspection).
# Usage (repository root, only Docker required): bash tests/e2e/run-e2e.sh [playwright args, e.g. -g "AT-2"]
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
export POSTGRES_PORT=${E2E_POSTGRES_PORT:-55432} REDIS_PORT=${E2E_REDIS_PORT:-56379}
export BACKEND_PORT=${E2E_BACKEND_PORT:-58080} FRONTEND_PORT=${E2E_FRONTEND_PORT:-53000}
COMPOSE=(docker compose -f "$ROOT/compose.yaml" -p home-quiz-e2e --profile e2e)

teardown() {
  if [ "${KEEP_STACK:-0}" = 1 ]; then
    echo "[e2e] stack kept: http://localhost:$FRONTEND_PORT/requests (remove: docker compose -p home-quiz-e2e --profile e2e down -v)"
  else
    "${COMPOSE[@]}" down -v > /dev/null 2>&1 || true
  fi
}
trap teardown EXIT

echo "[e2e] build images"
"${COMPOSE[@]}" build --quiet

echo "[e2e] run Playwright (starts postgres, redis, backend, frontend and seed first)"
set +e
"${COMPOSE[@]}" run --rm e2e "$@"
STATUS=$?
set -e
echo "[e2e] playwright exit $STATUS (report: tests/e2e/playwright-report/index.html)"
exit $STATUS
