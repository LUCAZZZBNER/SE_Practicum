#!/usr/bin/env bash

set -e
set -a
source "$(dirname "$0")/../.env"
set +a

cd "$(dirname "$0")"
SPRING_PROFILES_ACTIVE=test ./mvnw -B -ntp test
