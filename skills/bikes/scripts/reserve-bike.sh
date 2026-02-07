#!/usr/bin/env bash
# Reserve a bike for a user.
# Usage: reserve-bike.sh <bike_id> <user> [base_url]

set -euo pipefail

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
  echo "Error: bike_id and user are required"
  echo "Usage: reserve-bike.sh <bike_id> <user> [base_url]"
  exit 1
fi

BIKE_ID="$1"
USER="$2"
BASE_URL="${3:-http://localhost:8088}"

response=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -d "{\"user\": \"${USER}\"}" \
  "${BASE_URL}/bikes/${BIKE_ID}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ne 200 ]; then
  echo "Error: Failed to reserve bike '${BIKE_ID}' (HTTP $http_code)"
  echo "$body"
  exit 1
fi

echo "$body"
