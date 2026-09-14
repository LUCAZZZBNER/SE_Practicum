#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo_root"
: "${DELIVERY_BASE_URL:?Set DELIVERY_BASE_URL to the frontend /api/v1 origin}"
frontend_origin=${DELIVERY_FRONTEND_URL:-${DELIVERY_BASE_URL%/api/v1}}
python3 e2e/integration/test_frontend_gateway_workflow.py
DELIVERY_FRONTEND_URL="$frontend_origin" python3 e2e/integration/test_frontend_browser.py
(cd e2e/integration && python3 test_checkout_lifecycle.py)
