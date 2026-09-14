#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"

./services/identity-service/mvnw -f services/identity-service/pom.xml -B -ntp clean package -DskipTests
./services/media-service/mvnw -f services/media-service/pom.xml -B -ntp clean package -DskipTests
python3 e2e/integration/test_schema_ownership.py
python3 e2e/integration/test_database_privileges.py
INTERNAL_SERVICE_TOKEN="${INTERNAL_SERVICE_TOKEN:-test-service-token-01234567890123456789}" \
  python3 e2e/integration/test_media_isolation.py
