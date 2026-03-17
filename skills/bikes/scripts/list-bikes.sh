#!/usr/bin/env bash
# List all bikes and their status from the bikes service.
# Usage: list-bikes.sh [base_url]

set -euo pipefail

BASE_URL="${1:-http://localhost:8088}"

# Build traceparent header if available (W3C Trace Context propagation)
TRACE_HEADER=()
if [ -n "${TRACEPARENT:-}" ]; then
  TRACE_HEADER=(-H "traceparent: ${TRACEPARENT}")
fi

response=$(curl -s -w "\n%{http_code}" "${TRACE_HEADER[@]+"${TRACE_HEADER[@]}"}" "${BASE_URL}/bikes")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ne 200 ]; then
  echo "Error: Failed to fetch bikes (HTTP $http_code)"
  echo "$body"
  exit 1
fi

echo "$body"
