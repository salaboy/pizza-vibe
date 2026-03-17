#!/usr/bin/env bash
# Get the status of a specific bike, polling every 2 seconds until it is AVAILABLE.
# Usage: get-bike.sh <bike_id> [base_url]

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Error: bike_id is required"
  echo "Usage: get-bike.sh <bike_id> [base_url]"
  exit 1
fi

BIKE_ID="$1"
BASE_URL="${2:-http://localhost:8088}"
MAX_ATTEMPTS=30

# Build traceparent header if available (W3C Trace Context propagation)
TRACE_HEADER=()
if [ -n "${TRACEPARENT:-}" ]; then
  TRACE_HEADER=(-H "traceparent: ${TRACEPARENT}")
fi

for (( i=1; i<=MAX_ATTEMPTS; i++ )); do
  response=$(curl -s -w "\n%{http_code}" "${TRACE_HEADER[@]+"${TRACE_HEADER[@]}"}" "${BASE_URL}/bikes/${BIKE_ID}")
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [ "$http_code" -ne 200 ]; then
    echo "Error: Failed to fetch bike '${BIKE_ID}' (HTTP $http_code)"
    echo "$body"
    exit 1
  fi

  status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ "$status" = "AVAILABLE" ]; then
    echo "$body"
    exit 0
  fi

  sleep 2
done

echo "Error: Bike '${BIKE_ID}' did not become AVAILABLE after $((MAX_ATTEMPTS * 2)) seconds"
exit 1
