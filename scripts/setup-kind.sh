#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="pizza-vibe"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Pizza Vibe - KIND Setup ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# -------------------------------------------------------
# Pre-flight: Require ANTHROPIC_API_KEY
# -------------------------------------------------------
if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "ERROR: ANTHROPIC_API_KEY environment variable is not set."
  echo "The agent services require an Anthropic API key to function."
  echo ""
  echo "Set it before running this script:"
  echo "  export ANTHROPIC_API_KEY=<YOUR_KEY>"
  exit 1
fi

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
# 4. Create secrets
# -------------------------------------------------------
echo "--- Step 4: Creating secrets ---"
kubectl create secret generic anthropic-secret \
  --from-literal=api-key="$ANTHROPIC_API_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Secret 'anthropic-secret' created."
echo ""

# -------------------------------------------------------
# 5-9. Build, load images, and deploy
# -------------------------------------------------------
export CLUSTER_NAME
"$SCRIPT_DIR/rebuild-and-deploy.sh"

echo "Access the application with:"
echo "  kubectl port-forward svc/store 8080:8080"
echo "Then open http://localhost:8080"
echo ""
echo "To access PostgreSQL from outside the cluster:"
echo "  kubectl port-forward svc/postgresql 5432:5432"
echo "  psql postgres://postgres:postgres@localhost:5432/dapr_store"
