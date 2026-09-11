#!/usr/bin/env bash
set -Eeuo pipefail

base_url=${1:?Usage: verify-production.sh https://your-domain.example}
base_url=${base_url%/}

curl --fail --silent --show-error --retry 5 --retry-connrefused "$base_url/" >/dev/null
curl --fail --silent --show-error --retry 5 --retry-connrefused "$base_url/api/v1/health" >/dev/null

headers=$(curl --fail --silent --show-error --head "$base_url/")
printf '%s' "$headers" | grep -qi '^strict-transport-security:' || {
  printf 'Production verification failed: HTTPS response does not include HSTS.\n' >&2
  exit 1
}

printf 'Production storefront, API health endpoint and HTTPS headers are reachable.\n'
