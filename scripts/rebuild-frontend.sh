#!/usr/bin/env bash
set -euo pipefail

# Rebuild only the front-end (bundled inside the store image) and redeploy
# to an existing KIND cluster.

CLUSTER_NAME="${CLUSTER_NAME:-pizza-vibe}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Pizza Vibe - Front-end Rebuild & Deploy ==="
echo "Project root: $PROJECT_ROOT"
echo "KIND cluster: $CLUSTER_NAME"
echo ""

# -------------------------------------------------------
# 1. Build the store Docker image (includes front-end)
# -------------------------------------------------------
echo "--- Building store Docker image (includes front-end) ---"
cd "$PROJECT_ROOT"
docker build -t pizza-vibe-store:latest -f store/Dockerfile .
echo ""

# -------------------------------------------------------
# 2. Load image into KIND
# -------------------------------------------------------
echo "--- Loading image into KIND cluster ---"
kind load docker-image pizza-vibe-store:latest --name "$CLUSTER_NAME"
echo ""

# -------------------------------------------------------
# 3. Rollout restart the store deployment
# -------------------------------------------------------
echo "--- Restarting store deployment ---"
kubectl rollout restart deployment/store
echo ""

echo "--- Waiting for rollout to complete ---"
kubectl rollout status deployment/store --timeout=120s || true
kubectl get pods -l app=store
echo ""

echo "=== Front-end Rebuild & Deploy complete ==="
