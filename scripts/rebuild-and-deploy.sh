#!/usr/bin/env bash
set -euo pipefail

# Rebuild all images and redeploy to an existing KIND cluster.
# Can be run standalone or sourced by setup-kind.sh.

CLUSTER_NAME="${CLUSTER_NAME:-pizza-vibe}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Pizza Vibe - Rebuild & Deploy ==="
echo "Project root: $PROJECT_ROOT"
echo "KIND cluster: $CLUSTER_NAME"
echo ""


# -------------------------------------------------------
# 1. Clone and build a2a-java dependency
# -------------------------------------------------------
echo "--- Building a2a-java dependency ---"
A2A_JAVA_DIR="$PROJECT_ROOT/.deps/a2a-java"
if [ -d "$A2A_JAVA_DIR" ]; then
  echo "a2a-java already cloned, pulling latest changes..."
  (cd "$A2A_JAVA_DIR" && git pull)
else
  mkdir -p "$PROJECT_ROOT/.deps"
  git clone https://github.com/salaboy/a2a-java "$A2A_JAVA_DIR"
fi
(cd "$A2A_JAVA_DIR" && mvn clean install -DskipTests)
echo ""

# -------------------------------------------------------
# 2. Clone and build langchain4j-patched dependency
# -------------------------------------------------------
echo "--- Building langchain4j dependency ---"
LANGCHAIN4J_DIR="$PROJECT_ROOT/.deps/langchain4j"
if [ -d "$LANGCHAIN4J_DIR" ]; then
  echo "langchain4j already cloned, pulling latest changes..."
  (cd "$LANGCHAIN4J_DIR" && git pull)
else
  mkdir -p "$PROJECT_ROOT/.deps"
  git clone -b 1.11.0-beta19-patched https://github.com/salaboy/langchain4j "$LANGCHAIN4J_DIR"
fi
(cd "$LANGCHAIN4J_DIR" && mvn clean install -DskipTests)
echo ""

# -------------------------------------------------------
# 3. Clone and build quarkus-agentic-dapr dependency
# -------------------------------------------------------
echo "--- Building quarkus-agentic-dapr dependency ---"
AGENTIC_DAPR_DIR="$PROJECT_ROOT/.deps/quarkus-agentic-dapr"
if [ -d "$AGENTIC_DAPR_DIR" ]; then
  echo "quarkus-agentic-dapr already cloned, pulling latest changes..."
  (cd "$AGENTIC_DAPR_DIR" && git pull)
else
  mkdir -p "$PROJECT_ROOT/.deps"
  git clone https://github.com/salaboy/quarkus-agentic-dapr "$AGENTIC_DAPR_DIR"
fi
(cd "$AGENTIC_DAPR_DIR" && mvn clean install -DskipTests)
echo ""

# -------------------------------------------------------
# 2. Build agent services (Maven)
# -------------------------------------------------------
echo "--- Building agent services with Maven ---"
AGENTS=(pizza-mcp cooking-agent delivery-agent store-mgmt-agent)
for agent in "${AGENTS[@]}"; do
  echo "Building agents/$agent ..."
  (cd "$PROJECT_ROOT/agents/$agent" && ./mvnw clean package -DskipTests)
done
echo ""

# -------------------------------------------------------
# 3. Build Docker images
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
# 4. Load images into KIND
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
# 5. Deploy the application
# -------------------------------------------------------
echo "--- Deploying application ---"
kubectl apply -f "$PROJECT_ROOT/k8s/"

# Force pods to pick up the newly loaded images. Without this, Kubernetes
# sees the same ":latest" tag and keeps running the old containers.
echo "--- Restarting deployments to pick up new images ---"
kubectl rollout restart deployment
echo ""

echo "--- Waiting for rollout to complete ---"
kubectl rollout status deployment --timeout=120s || true
kubectl get pods
echo ""

echo "=== Rebuild & Deploy complete ==="
