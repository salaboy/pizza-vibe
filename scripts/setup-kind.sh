#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="pizza-vibe"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Pizza Vibe - KIND Setup ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# -------------------------------------------------------
# 1. Create KIND cluster
# -------------------------------------------------------
echo "--- Step 1: Creating KIND cluster '$CLUSTER_NAME' ---"
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  echo "Cluster '$CLUSTER_NAME' already exists, skipping creation."
else
  kind create cluster --name "$CLUSTER_NAME"
fi
kubectl cluster-info --context "kind-${CLUSTER_NAME}"
echo ""

# -------------------------------------------------------
# 2. Install Dapr
# -------------------------------------------------------
echo "--- Step 2: Installing Dapr ---"
helm repo add dapr https://dapr.github.io/helm-charts/ 2>/dev/null || true
helm repo update
if helm status dapr -n dapr-system &>/dev/null; then
  echo "Dapr is already installed, skipping."
else
  helm install dapr dapr/dapr --namespace dapr-system --create-namespace --wait
fi
echo "Dapr pods:"
kubectl get pods -n dapr-system
echo ""

# -------------------------------------------------------
# 3. Install PostgreSQL
# -------------------------------------------------------
echo "--- Step 3: Installing PostgreSQL ---"
helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update
if helm status postgresql &>/dev/null; then
  echo "PostgreSQL is already installed, skipping."
else
  helm install postgresql bitnami/postgresql \
    --set auth.postgresPassword=postgres \
    --set auth.database=dapr_store \
    --wait
fi
echo "PostgreSQL pods:"
kubectl get pods -l app.kubernetes.io/name=postgresql
echo ""

# -------------------------------------------------------
# 4. Clone and build quarkus-agentic-dapr dependency
# -------------------------------------------------------
echo "--- Step 4: Cloning and building quarkus-agentic-dapr ---"
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
# 5. Build agent services (Maven)
# -------------------------------------------------------
echo "--- Step 5: Building agent services with Maven ---"
AGENTS=(pizza-mcp cooking-agent delivery-agent store-mgmt-agent)
for agent in "${AGENTS[@]}"; do
  echo "Building agents/$agent ..."
  (cd "$PROJECT_ROOT/agents/$agent" && ./mvnw clean package -DskipTests)
done
echo ""

# -------------------------------------------------------
# 6. Build Docker images
# -------------------------------------------------------
echo "--- Step 6: Building Docker images ---"
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
# 7. Load images into KIND
# -------------------------------------------------------
echo "--- Step 7: Loading images into KIND cluster ---"
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
# 8. Create secrets
# -------------------------------------------------------
echo "--- Step 8: Creating secrets ---"
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "WARNING: ANTHROPIC_API_KEY environment variable is not set."
  echo "Set it and re-run, or create the secret manually:"
  echo "  kubectl create secret generic anthropic-secret --from-literal=api-key=<YOUR_KEY>"
else
  kubectl create secret generic anthropic-secret \
    --from-literal=api-key="$ANTHROPIC_API_KEY" \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "Secret 'anthropic-secret' created."
fi
echo ""

# -------------------------------------------------------
# 9. Deploy the application
# -------------------------------------------------------
echo "--- Step 9: Deploying application ---"
kubectl apply -f "$PROJECT_ROOT/k8s/"
echo ""

echo "--- Waiting for pods to be ready ---"
kubectl wait --for=condition=Ready pods --all --timeout=120s || true
kubectl get pods
echo ""

echo "=== Setup complete ==="
echo "Access the application with:"
echo "  kubectl port-forward svc/store 8080:8080"
echo "Then open http://localhost:8080"
