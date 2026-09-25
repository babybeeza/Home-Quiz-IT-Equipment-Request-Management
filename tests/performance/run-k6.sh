#!/usr/bin/env bash
# One ADR-007 run: reseed → start backend in the cache state → optional warm-up → k6 → cache metrics → stop.
# Usage (repository root, Compose postgres/redis running, backend jar built):
#   tests/performance/run-k6.sh <run-name> <vus> <duration> <disabled|cold|warm>
set -euo pipefail

RUN=$1 VUS=$2 DURATION=$3 CACHE=$4
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
OUT="$ROOT/tests/performance/results/TASK-007"
JAR="$ROOT/backend/target/equipment-request-service-0.0.1-SNAPSHOT.jar"
K6_IMAGE=grafana/k6:2.3.0
PORT=8080
mkdir -p "$OUT"

stop_backend() {
  powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort $PORT -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force }" || true
}

k6() {
  MSYS_NO_PATHCONV=1 docker run --rm -v "$(cd "$ROOT/tests/performance" && pwd -W):/perf" "$K6_IMAGE" run "$@"
}

stop_backend
echo "[$RUN] reseed"
docker compose -f "$ROOT/compose.yaml" exec -T postgres psql -q -U equipment_app -d equipment_requests -v ON_ERROR_STOP=1 \
  < "$ROOT/tests/performance/seed-search-dataset.sql" > /dev/null
echo "[$RUN] flush request-detail keys"
docker compose -f "$ROOT/compose.yaml" exec -T redis sh -c \
  "redis-cli --scan --pattern 'equipment:v1:request-detail:*' | xargs -r redis-cli del > /dev/null"

ENABLED=true
[ "$CACHE" = disabled ] && ENABLED=false
echo "[$RUN] start backend (caches enabled=$ENABLED)"
APP_CACHE_REQUEST_DETAIL_ENABLED=$ENABLED APP_CACHE_REFERENCE_DATA_ENABLED=$ENABLED \
  java -jar "$JAR" --server.port=$PORT > "$OUT/$RUN-backend.log" 2>&1 &
for _ in $(seq 1 90); do
  curl -sf "http://localhost:$PORT/actuator/health/liveness" > /dev/null 2>&1 && break
  sleep 1
done
curl -sf "http://localhost:$PORT/actuator/health/liveness" > /dev/null || { echo "backend did not start"; exit 1; }

if [ "$CACHE" = warm ]; then
  echo "[$RUN] warm-up pass (not measured)"
  k6 -e MODE=warmup --quiet /perf/workload.js > "$OUT/$RUN-warmup.txt" 2>&1
fi

echo "[$RUN] k6 run: $VUS VUs / $DURATION / cache=$CACHE"
set +e
k6 -e VUS="$VUS" -e DURATION="$DURATION" --summary-export "/perf/results/TASK-007/$RUN.json" /perf/workload.js \
  > "$OUT/$RUN.txt" 2>&1
K6_EXIT=$?
set -e

{
  echo "run=$RUN vus=$VUS duration=$DURATION cache=$CACHE k6_exit=$K6_EXIT"
  for result in hit miss error; do
    printf "request_detail %s: " "$result"
    curl -s "http://localhost:$PORT/actuator/metrics/equipment.cache.request_detail?tag=result:$result" \
      | grep -o '"value":[0-9.E]*' | head -1 || echo "n/a (cache disabled)"
    echo
  done
  for result in hit miss; do
    printf "reference-data %s: " "$result"
    curl -s "http://localhost:$PORT/actuator/metrics/cache.gets?tag=cache:reference-data&tag=result:$result" \
      | grep -o '"value":[0-9.E]*' | head -1 || echo "n/a (cache disabled)"
    echo
  done
  printf "redis request-detail keys after run: "
  docker compose -f "$ROOT/compose.yaml" exec -T redis sh -c "redis-cli --scan --pattern 'equipment:v1:request-detail:*' | wc -l"
} > "$OUT/$RUN-cache.txt"

stop_backend
echo "[$RUN] done: k6 exit $K6_EXIT (99 = thresholds crossed)"
exit 0
