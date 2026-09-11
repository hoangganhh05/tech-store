#!/usr/bin/env bash
set -Eeuo pipefail

project_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$project_dir"

if ! git diff --quiet || ! git diff --cached --quiet; then
  printf 'Deployment aborted: the server checkout has uncommitted changes.\n' >&2
  exit 1
fi

git switch main
git pull --ff-only origin main

./scripts/preflight-production-server.sh

docker compose --env-file .env \
  -f docker-compose.yml \
  -f deploy/docker-compose.production.yml \
  up --build --detach --remove-orphans --wait --wait-timeout 240

app_domain=$(grep -E '^APP_DOMAIN=' .env | tail -n 1 | cut -d '=' -f2-)
./scripts/verify-production.sh "https://${app_domain}"
