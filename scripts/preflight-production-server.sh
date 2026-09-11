#!/usr/bin/env bash
set -Eeuo pipefail

fail() {
  printf 'Preflight failed: %s\n' "$1" >&2
  exit 1
}

command -v docker >/dev/null || fail 'Docker Engine is not installed.'
docker compose version >/dev/null || fail 'Docker Compose plugin is not installed.'
command -v git >/dev/null || fail 'git is not installed.'
command -v curl >/dev/null || fail 'curl is not installed.'

for port in 80 443; do
  if command -v ss >/dev/null && ss -ltn "sport = :$port" | grep -q LISTEN; then
    fail "port $port is already in use; stop the conflicting web server before deployment."
  fi
done

[ -f .env ] || fail 'Missing .env. Copy .env.example and fill all required production values.'

required=(APP_DOMAIN ACME_EMAIL JWT_SECRET MYSQL_PASSWORD MYSQL_ROOT_PASSWORD CORS_ALLOWED_ORIGINS PASSWORD_RESET_FRONTEND_URL MAIL_HOST MAIL_USERNAME MAIL_PASSWORD MAIL_FROM)
for key in "${required[@]}"; do
  value=$(grep -E "^${key}=" .env | tail -n 1 | cut -d '=' -f2- || true)
  [ -n "$value" ] || fail "${key} is empty in .env."
  case "$value" in
    *example.com*) fail "${key} still contains the example domain; replace it with the real production value." ;;
  esac
done

printf 'Production server preflight passed.\n'
