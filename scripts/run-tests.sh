#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Copy .env.example to .env and set local credentials first." >&2
  exit 1
fi

# Make Compose credentials available to the child processes.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is missing from .env}"

cd "$ROOT_DIR"

echo "Starting the Docker database and application services..."
docker compose up -d

echo "Waiting for MySQL to become healthy..."
for attempt in $(seq 1 30); do
  if docker compose ps --status running --services | grep -qx db \
      && docker compose exec -T db sh -c 'mysqladmin ping -h 127.0.0.1 -u root -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    echo "MySQL did not become ready in time." >&2
    docker compose logs --no-color db >&2 || true
    exit 1
  fi
  sleep 2
done

echo "Waiting for the frontend proxy and domain services..."
frontend_origin="http://localhost:${FRONTEND_PORT:-5173}"
ready_checks=0
for attempt in $(seq 1 60); do
  status=$(curl -sS -o /dev/null -w '%{http_code}' "$frontend_origin/api/v1/cart-items" || true)
  if [[ "$status" == "401" || "$status" == "403" || "$status" == "200" ]]; then
    ready_checks=$((ready_checks + 1))
    if [[ "$ready_checks" -ge 3 ]]; then break; fi
  else
    ready_checks=0
  fi
  if [[ "$attempt" -eq 60 ]]; then
    echo "The frontend proxy did not reach the domain services in time." >&2
    docker compose ps >&2 || true
    exit 1
  fi
  sleep 2
done

echo "Running frontend tests..."
(
  cd "$ROOT_DIR/frontend"
  if [[ ! -d node_modules ]]; then
    npm ci
  fi
  npm run test:run
)

echo "Running end-to-end business-risk checks..."
(
  cd "$ROOT_DIR"
  DELIVERY_BASE_URL="http://localhost:${FRONTEND_PORT:-5173}/api/v1" \
    python3 e2e/scenarios/backend_business_risks.py
)

echo "Running frontend feature-matrix integration check..."
(
  cd "$ROOT_DIR"
  DELIVERY_BASE_URL="http://localhost:${FRONTEND_PORT:-5173}/api/v1" \
    python3 e2e/integration/test_frontend_feature_matrix.py
)

echo "All tests passed. Docker services remain running."
