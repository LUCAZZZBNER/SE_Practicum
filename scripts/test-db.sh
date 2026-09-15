#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE_DIR="${TEST_DB_FIXTURE_DIR:-$ROOT_DIR/tmp/test-db-fixtures}"
SERVICES=(identity catalog cart order media)
snapshot_tmp=''
test_container=''

usage() {
  cat <<'EOF'
Usage:
  scripts/test-db.sh snapshot
      Capture the five populated delivery_* databases from the running Compose
      db. Pause application writers first for a consistent cross-service fixture.
      Snapshots include schema, Flyway history, and data. Existing snapshots
      cannot be overwritten; choose a new directory when refreshing fixtures.
  scripts/test-db.sh run -- <command> [args...]
      Restore the snapshot into a new MySQL container with a random local port.
      Export each service's *_DATASOURCE_URL and *_DB_USERNAME/PASSWORD, then run
      the command. Delete the container and its data on success, failure, or
      interruption. Services started by the command must inherit these settings.

Examples:
  scripts/test-db.sh snapshot
  scripts/test-db.sh run -- services/identity-service/mvnw -f services/identity-service/pom.xml test
  scripts/test-db.sh run -- bash

Environment:
  TEST_DB_FIXTURE_DIR       Local snapshot directory (default: tmp/test-db-fixtures)
  TEST_DB_SOURCE_CONTAINER  Snapshot from this container instead of Compose db
  TEST_DB_MYSQL_IMAGE       Disposable MySQL image (default: mysql:8.4)

The run command needs Docker and openssl. It does not start or reconfigure the
development stack. Existing HTTP tests against a running gateway keep using that
gateway's database; start the tested services inside this wrapper to isolate them.
EOF
}

cleanup() {
  local status=$?
  trap - EXIT
  if [[ -n "$test_container" ]]; then
    if ! docker rm -fv "$test_container" >/dev/null; then
      echo "Could not remove disposable container $test_container." >&2
      status=1
    fi
  fi
  if [[ -n "$snapshot_tmp" ]]; then
    rm -rf -- "$snapshot_tmp"
  fi
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

snapshot() {
  if [[ -e "$FIXTURE_DIR" ]]; then
    echo "Snapshot already exists: $FIXTURE_DIR. Choose a new TEST_DB_FIXTURE_DIR to refresh it." >&2
    exit 2
  fi
  mkdir -p "$(dirname "$FIXTURE_DIR")"
  snapshot_tmp="$(mktemp -d "$(dirname "$FIXTURE_DIR")/.test-db-fixture.XXXXXX")"
  local -a source_exec
  if [[ -n "${TEST_DB_SOURCE_CONTAINER:-}" ]]; then
    source_exec=(docker exec -i "$TEST_DB_SOURCE_CONTAINER")
  else
    source_exec=(docker compose --project-directory "$ROOT_DIR" exec -T db)
  fi

  local service
  for service in "${SERVICES[@]}"; do
    echo "Snapshotting delivery_$service"
    "${source_exec[@]}" sh -c '
      export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
      exec mysqldump --protocol=socket -uroot --single-transaction --skip-lock-tables \
        --no-tablespaces --set-gtid-purged=OFF --hex-blob --skip-comments "$1"
    ' sh "delivery_$service" > "$snapshot_tmp/$service.sql"
  done
  (cd "$snapshot_tmp" && sha256sum identity.sql catalog.sql cart.sql order.sql media.sql > SHA256SUMS)
  # Publish only a complete snapshot. The run command also verifies these hashes.
  chmod 400 "$snapshot_tmp"/*
  mv -T -- "$snapshot_tmp" "$FIXTURE_DIR"
  snapshot_tmp=''
  echo "Fixture snapshot saved to $FIXTURE_DIR"
}

verify_snapshot() {
  local service
  for service in "${SERVICES[@]}"; do
    if [[ ! -s "$FIXTURE_DIR/$service.sql" ]]; then
      echo "Missing fixture $FIXTURE_DIR/$service.sql. Run '$0 snapshot' after preparing the data." >&2
      exit 2
    fi
  done
  (cd "$FIXTURE_DIR" && sha256sum --check --status SHA256SUMS)
}

run_tests() {
  if [[ "${1:-}" == '--' ]]; then shift; fi
  if [[ "$#" -eq 0 ]]; then usage >&2; exit 2; fi
  verify_snapshot
  local root_password="$(openssl rand -hex 24)"
  local candidate="delivery-test-db-$(openssl rand -hex 8)"
  MYSQL_ROOT_PASSWORD="$root_password" docker run -d --name "$candidate" \
    --label delivery.test-db=true --env MYSQL_ROOT_PASSWORD \
    --publish 127.0.0.1::3306 "${TEST_DB_MYSQL_IMAGE:-mysql:8.4}" \
    --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci >/dev/null
  test_container="$candidate"

  local -a mysql=(docker exec -i -e "MYSQL_PWD=$root_password" "$test_container" mysql --protocol=socket -uroot)
  local ready=false
  for ((attempt=0; attempt<90; attempt++)); do
    # TCP becomes available only after the image's temporary initialization server exits.
    if docker exec -e "MYSQL_PWD=$root_password" "$test_container" \
        mysql --protocol=tcp -h127.0.0.1 -uroot -e 'SELECT 1' >/dev/null 2>&1; then
      ready=true
      break
    fi
    sleep 1
  done
  if [[ "$ready" != true ]]; then
    echo "Disposable MySQL did not become ready." >&2
    docker logs --tail 40 "$test_container" >&2
    exit 1
  fi
  local port
  port="$(docker port "$test_container" 3306/tcp)"
  port="${port##*:}"
  local service upper password
  for service in "${SERVICES[@]}"; do
    "${mysql[@]}" -e "CREATE DATABASE delivery_$service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    "${mysql[@]}" "delivery_$service" < "$FIXTURE_DIR/$service.sql"
    password="$(openssl rand -hex 24)"
    # Service credentials only have privileges inside their own disposable schema.
    "${mysql[@]}" <<SQL
CREATE USER '${service}_test'@'%' IDENTIFIED BY '$password';
GRANT ALL PRIVILEGES ON delivery_${service}.* TO '${service}_test'@'%';
SQL
    upper="${service^^}"
    export "${upper}_DB_USERNAME=${service}_test" "${upper}_DB_PASSWORD=$password"
    export "${upper}_DATASOURCE_URL=jdbc:mysql://127.0.0.1:$port/delivery_$service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
  done
  export TEST_DB_CONTAINER="$test_container" TEST_DB_PORT="$port"
  echo "Running against $test_container on 127.0.0.1:$port"
  local status=0
  (
    # Avoid generic Spring datasource overrides inherited from a development shell.
    unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
    unset SPRING_FLYWAY_URL SPRING_FLYWAY_USER SPRING_FLYWAY_PASSWORD MYSQL_ROOT_PASSWORD
    "$@"
  ) || status=$?
  verify_snapshot
  return "$status"
}

case "${1:-}" in
  snapshot) snapshot ;;
  run) shift; run_tests "$@" ;;
  -h|--help|'') usage ;;
  *) echo "Unknown command: $1" >&2; usage >&2; exit 2 ;;
esac
