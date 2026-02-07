#!/usr/bin/env bash
# Get the status of a specific bike.
# Usage: get-bike.sh <bike_id> [base_url]

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Error: bike_id is required"
  echo "Usage: get-bike.sh <bike_id> [base_url]"
  exit 1
fi

BIKE_ID="$1"
BASE_URL="${2:-http://localhost:8088}"

response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/bikes/${BIKE_ID}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ne 200 ]; then
  echo "Error: Failed to fetch bike '${BIKE_ID}' (HTTP $http_code)"
  echo "$body"
  exit 1
fi

echo "$body"
