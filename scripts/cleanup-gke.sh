#!/usr/bin/env bash
set -uo pipefail

# Cleanup script for Pizza Vibe on GKE.
# Removes everything installed by setup-gke.sh in reverse order.
#
# Required environment variables:
#   GKE_CLUSTER  - GKE cluster name
#   GKE_REGION   - GKE cluster region (e.g., us-central1)
#   GCP_PROJECT  - Google Cloud project ID

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OBSERVABILITY_DIR="$PROJECT_ROOT/k8s-observability"

echo "=== Pizza Vibe - GKE Cleanup ==="
echo "Project root: $PROJECT_ROOT"
echo ""

# -------------------------------------------------------
# Pre-flight checks
# -------------------------------------------------------
for cmd in gcloud kubectl helm; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' is required but not found. Please install it and retry."
    exit 1
  fi
done

for var in GKE_CLUSTER GKE_REGION GCP_PROJECT; do
  if [ -z "${!var:-}" ]; then
    echo "ERROR: Environment variable $var is not set."
    exit 1
  fi
done

echo "Cluster : $GKE_CLUSTER"
echo "Region  : $GKE_REGION"
echo "Project : $GCP_PROJECT"
echo ""

# -------------------------------------------------------
# 1. Authenticate kubectl to the GKE cluster
# -------------------------------------------------------
echo "--- Step 1: Authenticating to GKE cluster '$GKE_CLUSTER' ---"
gcloud container clusters get-credentials "$GKE_CLUSTER" \
  --region "$GKE_REGION" \
  --project "$GCP_PROJECT"
echo ""

# -------------------------------------------------------
# 2. Remove application services (step 6 in setup)
# -------------------------------------------------------
echo "--- Step 2: Removing application services ---"
kubectl delete -f "$PROJECT_ROOT/k8s/" --ignore-not-found=true
echo ""

# -------------------------------------------------------
# 3. Remove secrets (step 4 in setup)
# -------------------------------------------------------
echo "--- Step 3: Removing secrets ---"
kubectl delete secret anthropic-secret --ignore-not-found=true
echo ""

# -------------------------------------------------------
# 4. Remove OTel Instrumentation resource (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 4: Removing OTel Instrumentation resource ---"
kubectl delete -f "$OBSERVABILITY_DIR/instrumentation.yaml" --ignore-not-found=true
echo ""

# -------------------------------------------------------
# 5. Uninstall OpenTelemetry Operator (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 5: Uninstalling OpenTelemetry Operator ---"
if helm status opentelemetry-operator -n opentelemetry &>/dev/null; then
  helm uninstall opentelemetry-operator -n opentelemetry
else
  echo "OpenTelemetry Operator not found, skipping."
fi
echo ""

# -------------------------------------------------------
# 6. Uninstall OpenTelemetry Collector (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 6: Uninstalling OpenTelemetry Collector ---"
if helm status otel-collector -n opentelemetry &>/dev/null; then
  helm uninstall otel-collector -n opentelemetry
else
  echo "OTel Collector not found, skipping."
fi
echo ""

# -------------------------------------------------------
# 7. Remove Dash0 secret and opentelemetry namespace (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 7: Removing opentelemetry namespace and Dash0 secret ---"
kubectl delete secret dash0-secrets -n opentelemetry --ignore-not-found=true
kubectl delete namespace opentelemetry --ignore-not-found=true
echo ""

# -------------------------------------------------------
# 8. Uninstall cert-manager (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 8: Uninstalling cert-manager ---"
if helm status cert-manager -n cert-manager &>/dev/null; then
  helm uninstall cert-manager -n cert-manager
  kubectl delete namespace cert-manager --ignore-not-found=true
else
  echo "cert-manager not found, skipping."
fi
# Remove cert-manager webhook configurations if still present
kubectl delete validatingwebhookconfiguration cert-manager-webhook 2>/dev/null || true
kubectl delete mutatingwebhookconfiguration cert-manager-webhook 2>/dev/null || true
echo ""

# -------------------------------------------------------
# 9. Uninstall Jaeger (step 5 in setup)
# -------------------------------------------------------
echo "--- Step 9: Uninstalling Jaeger ---"
if helm status jaeger &>/dev/null; then
  helm uninstall jaeger
else
  echo "Jaeger not found, skipping."
fi
echo ""

# -------------------------------------------------------
# 10. Uninstall PostgreSQL (step 3 in setup)
# -------------------------------------------------------
echo "--- Step 10: Uninstalling PostgreSQL ---"
if helm status postgresql &>/dev/null; then
  helm uninstall postgresql
  # Remove the PVC left behind by the StatefulSet
  kubectl delete pvc -l app.kubernetes.io/name=postgresql --ignore-not-found=true
else
  echo "PostgreSQL not found, skipping."
fi
echo ""

echo "=== Cleanup complete ==="
echo ""
echo "All Pizza Vibe resources have been removed from cluster '$GKE_CLUSTER'."
echo "Run 'kubectl get all' to verify the cluster is clean."
