#!/usr/bin/env bash
set -euo pipefail

# Setup Pizza Vibe on an existing GKE cluster.
# Deploys pre-built images from Docker Hub (salaboy/pizza-vibe-*:latest)
# published by the GitHub Actions workflow.
#
# Required environment variables:
#   GKE_CLUSTER       - GKE cluster name
#   GKE_REGION        - GKE cluster region (e.g., us-central1)
#   GCP_PROJECT       - Google Cloud project ID
#   ANTHROPIC_API_KEY - Anthropic API key for the agent services
#
# Optional environment variables:
#   DASH0_AUTH_TOKEN                   - Dash0 auth token (enables Dash0 export alongside Jaeger)
#   DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME  - defaults to ingress.eu-west-1.aws.dash0.com
#   DASH0_ENDPOINT_OTLP_GRPC_PORT      - defaults to 4317
#   DASH0_DATASET                      - defaults to salaboy

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OBSERVABILITY_DIR="$PROJECT_ROOT/k8s-observability"

echo "=== Pizza Vibe - GKE Setup ==="
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

for var in GKE_CLUSTER GKE_REGION GCP_PROJECT ANTHROPIC_API_KEY; do
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
kubectl cluster-info
echo ""

# -------------------------------------------------------
# 2. Register Helm repositories
# -------------------------------------------------------
echo "--- Step 2: Registering Helm repositories ---"
helm repo add bitnami        https://charts.bitnami.com/bitnami                          2>/dev/null || true
helm repo add jaegertracing  https://jaegertracing.github.io/helm-charts                 2>/dev/null || true
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts  2>/dev/null || true
helm repo add jetstack       https://charts.jetstack.io                                  --force-update
helm repo update
echo ""

# -------------------------------------------------------
# 3. Install PostgreSQL
# -------------------------------------------------------
echo "--- Step 3: Installing PostgreSQL ---"
if helm status postgresql &>/dev/null; then
  echo "PostgreSQL is already installed, skipping."
else
  helm install postgresql bitnami/postgresql \
    --set auth.postgresPassword=postgres \
    --set auth.database=store-db \
    --wait
fi
kubectl get pods -l app.kubernetes.io/name=postgresql
echo ""

# -------------------------------------------------------
# 4. Create secrets
# -------------------------------------------------------
echo "--- Step 4: Creating secrets ---"
kubectl create secret generic anthropic-secret \
  --from-literal=api-key="$ANTHROPIC_API_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Secret 'anthropic-secret' applied."
echo ""

# -------------------------------------------------------
# 5. Install Observability stack
# -------------------------------------------------------
echo "--- Step 5: Installing Observability stack ---"

# Jaeger (all-in-one, memory storage)
if helm status jaeger &>/dev/null; then
  echo "Jaeger is already installed, skipping."
else
  helm install jaeger jaegertracing/jaeger --version 3.4.1 \
    -f "$OBSERVABILITY_DIR/jaeger-values.yaml" \
    --wait
fi
kubectl get pods -l app.kubernetes.io/name=jaeger
echo ""

# OpenTelemetry namespace + optional Dash0 secret
kubectl create namespace opentelemetry --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace opentelemetry \
  pod-security.kubernetes.io/enforce=privileged \
  --overwrite

if [ -n "${DASH0_AUTH_TOKEN:-}" ]; then
  DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME="${DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME:-ingress.eu-west-1.aws.dash0.com}"
  DASH0_ENDPOINT_OTLP_GRPC_PORT="${DASH0_ENDPOINT_OTLP_GRPC_PORT:-4317}"
  DASH0_DATASET="${DASH0_DATASET:-salaboy}"

  kubectl create secret generic dash0-secrets \
    --from-literal=dash0-authorization-token="$DASH0_AUTH_TOKEN" \
    --from-literal=dash0-grpc-hostname="$DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME" \
    --from-literal=dash0-grpc-port="$DASH0_ENDPOINT_OTLP_GRPC_PORT" \
    --from-literal=dash0-dataset="$DASH0_DATASET" \
    --namespace=opentelemetry \
    --dry-run=client -o yaml | kubectl apply -f -
  # Also create in default namespace so the store deployment can read it.
  kubectl create secret generic dash0-secrets \
    --from-literal=dash0-authorization-token="$DASH0_AUTH_TOKEN" \
    --from-literal=dash0-grpc-hostname="$DASH0_ENDPOINT_OTLP_GRPC_HOSTNAME" \
    --from-literal=dash0-grpc-port="$DASH0_ENDPOINT_OTLP_GRPC_PORT" \
    --from-literal=dash0-dataset="$DASH0_DATASET" \
    --namespace=default \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "Dash0 secrets applied. Collector will export to Jaeger + Dash0."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config.yaml"
else
  echo "DASH0_AUTH_TOKEN not set. Collector will export to Jaeger only."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config-jaeger-only.yaml"
fi

if helm status otel-collector -n opentelemetry &>/dev/null; then
  helm upgrade otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry -f "$COLLECTOR_VALUES" --wait
else
  helm install otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry -f "$COLLECTOR_VALUES" --wait
fi
# Force a pod restart so updated secret values (endpoint, dataset, token) are reloaded.
COLLECTOR_DEPLOY=$(kubectl get deployment -n opentelemetry \
  -l app.kubernetes.io/name=opentelemetry-collector \
  -o jsonpath='{.items[0].metadata.name}')
kubectl rollout restart deployment/"$COLLECTOR_DEPLOY" -n opentelemetry
kubectl rollout status deployment/"$COLLECTOR_DEPLOY" -n opentelemetry --timeout=120s
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-collector
echo ""

# cert-manager (required by OTel Operator)
if helm status cert-manager -n cert-manager &>/dev/null; then
  echo "cert-manager is already installed, skipping."
else
  # On GKE Autopilot a previous failed install can leave behind webhook configs
  # whose CA bundle no longer matches any running pod. Remove them so Helm can
  # re-create them cleanly (cert-manager will re-register them on startup).
  if kubectl get validatingwebhookconfiguration cert-manager-webhook &>/dev/null; then
    RUNNING=$(kubectl get pods -n cert-manager -l app.kubernetes.io/instance=cert-manager \
      --no-headers 2>/dev/null | grep -c Running || true)
    if [ "$RUNNING" -eq 0 ]; then
      echo "Removing stale cert-manager webhook configurations..."
      kubectl delete validatingwebhookconfiguration cert-manager-webhook 2>/dev/null || true
      kubectl delete mutatingwebhookconfiguration cert-manager-webhook 2>/dev/null || true
    fi
  fi

  helm upgrade --install cert-manager jetstack/cert-manager \
    --namespace cert-manager --create-namespace \
    --set crds.enabled=true \
    --set global.leaderElection.namespace=cert-manager \
    --set resources.requests.cpu=250m \
    --set resources.requests.memory=256Mi \
    --set webhook.resources.requests.cpu=250m \
    --set webhook.resources.requests.memory=128Mi \
    --set cainjector.resources.requests.cpu=250m \
    --set cainjector.resources.requests.memory=256Mi \
    --set startupapicheck.resources.requests.cpu=100m \
    --set startupapicheck.resources.requests.memory=64Mi \
    --timeout 10m \
    --wait

  # GKE Autopilot needs extra time to inject the CA bundle into the webhook.
  # Wait until the webhook deployment is Available before any consumer (OTel
  # Operator) tries to create cert-manager resources.
  echo "Waiting for cert-manager webhook to become available..."
  kubectl wait deployment/cert-manager-webhook \
    --for=condition=Available \
    --namespace cert-manager \
    --timeout=120s
  echo "Pausing 20 s for CA bundle injection to propagate..."
  sleep 20
fi
kubectl get pods -n cert-manager
echo ""

# OpenTelemetry Operator
# NOTE: Go auto-instrumentation uses eBPF. On GKE this works with Container-Optimized
# OS nodes. If pods fail to start, verify your node pool uses COS and that the
# opentelemetry-operator daemonset has the required privileges.
if helm status opentelemetry-operator -n opentelemetry &>/dev/null; then
  echo "OpenTelemetry Operator is already installed, skipping."
else
  helm upgrade --install opentelemetry-operator open-telemetry/opentelemetry-operator \
    --namespace opentelemetry \
    --set manager.extraArgs='{--enable-go-instrumentation}' \
    --wait
fi
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-operator

# Wait for cert-manager to issue the operator's webhook TLS certificate.
# The operator pod will fail with FailedMount until this secret exists.
echo "Waiting for cert-manager to issue the opentelemetry-operator webhook certificate..."
CERT_SECRET="opentelemetry-operator-controller-manager-service-cert"
for i in $(seq 1 30); do
  if kubectl get secret "$CERT_SECRET" -n opentelemetry &>/dev/null; then
    echo "Certificate secret '$CERT_SECRET' found."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERROR: Certificate secret '$CERT_SECRET' not found after 5 minutes."
    echo "Check cert-manager logs: kubectl logs -n cert-manager -l app.kubernetes.io/name=cert-manager"
    exit 1
  fi
  echo "  ... attempt $i/30, retrying in 10s"
  sleep 10
done
echo ""

# Wait for the OTel operator webhook to have live endpoints before applying
# Instrumentation resources. The deployment being Available does not guarantee
# the webhook server is accepting connections yet.
echo "Waiting for OpenTelemetry operator webhook endpoints to become ready..."
for i in $(seq 1 24); do
  ENDPOINTS=$(kubectl get endpoints opentelemetry-operator-webhook \
    -n opentelemetry -o jsonpath='{.subsets[*].addresses}' 2>/dev/null || true)
  if [ -n "$ENDPOINTS" ]; then
    echo "Webhook endpoints ready."
    break
  fi
  echo "  ... attempt $i/24, retrying in 5s"
  sleep 5
done

kubectl apply -f "$OBSERVABILITY_DIR/instrumentation.yaml"
echo "OTel Instrumentation resource applied."
echo ""

# -------------------------------------------------------
# 6. Deploy application services
# -------------------------------------------------------
echo "--- Step 6: Deploying application services ---"
kubectl apply -f "$PROJECT_ROOT/k8s/"
echo "Restarting deployments to pull fresh images..."
kubectl rollout restart deployment
echo ""

echo "Waiting for rollout to complete..."
kubectl rollout status deployment --timeout=180s || true
kubectl get pods
echo ""

# -------------------------------------------------------
# 7. Wait for the store LoadBalancer IP
# -------------------------------------------------------
# The store service is the single public entry point — it serves the
# static front-end and proxies all /api/* calls to internal services.
# All other services use ClusterIP (cluster-internal only).
echo "--- Step 7: Waiting for store LoadBalancer IP ---"

echo "Waiting for store LoadBalancer IP to be assigned (this can take ~60s)..."
for i in $(seq 1 18); do
  STORE_IP="$(kubectl get svc store -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  [ -n "$STORE_IP" ] && break
  echo "  ... attempt $i/18, retrying in 10s"
  sleep 10
done

echo ""
echo "=== Setup complete ==="
echo ""
if [ -n "${STORE_IP:-}" ]; then
  echo "Pizza Vibe: http://$STORE_IP"
else
  echo "Store IP not yet assigned — check with: kubectl get svc store"
fi
echo ""
echo "Jaeger UI (port-forward):"
echo "  kubectl port-forward svc/jaeger-query 16686"
echo "  http://localhost:16686"
echo ""
echo "PostgreSQL (port-forward):"
echo "  kubectl port-forward svc/postgresql 5432:5432"
echo "  psql postgres://postgres:postgres@localhost:5432/store-db"
