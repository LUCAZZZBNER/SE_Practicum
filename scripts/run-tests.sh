#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Copy .env.example to .env and set local credentials first." >&2
  exit 1
fi

# Make the database credentials available to Maven and to child processes.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${DELIVERY_DB_USERNAME:?DELIVERY_DB_USERNAME is missing from .env}"
: "${DELIVERY_DB_PASSWORD:?DELIVERY_DB_PASSWORD is missing from .env}"
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

echo "Ensuring the test database exists..."
docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS delivery_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON delivery_test.* TO \"$MYSQL_USER\"@\"%\"; FLUSH PRIVILEGES;"' >/dev/null

echo "Running backend tests..."
(
  cd "$ROOT_DIR/backend"
  SPRING_PROFILES_ACTIVE=test ./mvnw -B -ntp test
)

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
  python3 e2e/scenarios/backend_business_risks.py
)

echo "All tests passed. Docker services remain running."
