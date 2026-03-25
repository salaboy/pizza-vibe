#!/usr/bin/env bash
# Reserve a bike for a user.
# Usage: reserve-bike.sh <bike_id> <user> [order_id] [base_url]

set -euo pipefail

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
  echo "Error: bike_id and user are required"
  echo "Usage: reserve-bike.sh <bike_id> <user> [order_id] [base_url]"
  exit 1
fi

BIKE_ID="$1"
USER="$2"
ORDER_ID="${3:-}"
BASE_URL="${4:-http://localhost:8088}"

if [ -n "$ORDER_ID" ]; then
  PAYLOAD="{\"user\": \"${USER}\", \"orderId\": \"${ORDER_ID}\"}"
else
  PAYLOAD="{\"user\": \"${USER}\"}"
fi

# Build traceparent header if available (W3C Trace Context propagation)
TRACE_HEADER=()
if [ -n "${TRACEPARENT:-}" ]; then
  TRACE_HEADER=(-H "traceparent: ${TRACEPARENT}")
fi

response=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  "${TRACE_HEADER[@]+"${TRACE_HEADER[@]}"}" \
  -d "$PAYLOAD" \
  "${BASE_URL}/bikes/${BIKE_ID}")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ne 200 ]; then
  echo "Error: Failed to reserve bike '${BIKE_ID}' (HTTP $http_code)"
  echo "$body"
  exit 1
fi

echo "$body"
