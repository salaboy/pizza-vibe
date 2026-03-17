#!/usr/bin/env bash
set -euo pipefail

# Quick rebuild & deploy: skips building external Java dependencies
# (a2a-java, langchain4j, quarkus-agentic-dapr).
# Use the full rebuild-and-deploy.sh if you need to rebuild those.

CLUSTER_NAME="${CLUSTER_NAME:-pizza-vibe}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Pizza Vibe - Quick Rebuild & Deploy ==="
echo "Project root: $PROJECT_ROOT"
echo "KIND cluster: $CLUSTER_NAME"
echo "(Skipping a2a-java, langchain4j, quarkus-agentic-dapr builds)"
echo ""

# -------------------------------------------------------
# 1. Build agent services (Maven)
# -------------------------------------------------------
echo "--- Building agent services with Maven ---"
AGENTS=(pizza-mcp cooking-agent delivery-agent store-mgmt-agent)
for agent in "${AGENTS[@]}"; do
  echo "Building agents/$agent ..."
  (cd "$PROJECT_ROOT/agents/$agent" && ./mvnw clean package -DskipTests)
done
echo ""

# -------------------------------------------------------
# 2. Build Docker images
# -------------------------------------------------------
echo "--- Building Docker images ---"
cd "$PROJECT_ROOT"

# Store image (includes front-end static export)
docker build -t pizza-vibe-store:latest -f store/Dockerfile .

# Go services
docker build -t pizza-vibe-inventory:latest -f inventory/Dockerfile .
docker build -t pizza-vibe-oven:latest -f oven/Dockerfile .
docker build -t pizza-vibe-bikes:latest -f bikes/Dockerfile .
docker build -t pizza-vibe-drinks-stock:latest -f drinks-stock/Dockerfile .

# Java/Quarkus agent services
docker build -t pizza-vibe-pizza-mcp:latest -f agents/pizza-mcp/src/main/docker/Dockerfile.jvm ./agents/pizza-mcp
docker build -t pizza-vibe-cooking-agent:latest -f agents/cooking-agent/src/main/docker/Dockerfile.jvm ./agents/cooking-agent
docker build -t pizza-vibe-delivery-agent:latest -f agents/delivery-agent/src/main/docker/Dockerfile.jvm .
docker build -t pizza-vibe-store-mgmt-agent:latest -f agents/store-mgmt-agent/src/main/docker/Dockerfile.jvm ./agents/store-mgmt-agent
echo ""

# -------------------------------------------------------
# 3. Load images into KIND
# -------------------------------------------------------
echo "--- Loading images into KIND cluster ---"
IMAGES=(
  pizza-vibe-store
  pizza-vibe-inventory
  pizza-vibe-oven
  pizza-vibe-bikes
  pizza-vibe-drinks-stock
  pizza-vibe-pizza-mcp
  pizza-vibe-cooking-agent
  pizza-vibe-delivery-agent
  pizza-vibe-store-mgmt-agent
)
for image in "${IMAGES[@]}"; do
  echo "Loading $image:latest ..."
  kind load docker-image "$image:latest" --name "$CLUSTER_NAME"
done
echo ""

# -------------------------------------------------------
# 4. Deploy the application
# -------------------------------------------------------
echo "--- Deploying application ---"
kubectl apply -f "$PROJECT_ROOT/k8s/"

# Force pods to pick up the newly loaded images
echo "--- Restarting deployments to pick up new images ---"
kubectl rollout restart deployment
echo ""

echo "--- Waiting for rollout to complete ---"
kubectl rollout status deployment --timeout=120s || true
kubectl get pods
echo ""

echo "=== Quick Rebuild & Deploy complete ==="
