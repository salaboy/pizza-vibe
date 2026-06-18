#!/usr/bin/env bash
set -euo pipefail

# Setup Pizza Vibe on an existing GKE cluster.
#
# Required environment variables:
#   GKE_CLUSTER       - GKE cluster name
#   GKE_REGION        - GKE cluster region (e.g., us-central1)
#   GCP_PROJECT       - Google Cloud project ID
#   REGISTRY          - Artifact Registry repository base path
#                       e.g., us-central1-docker.pkg.dev/my-project/pizza-vibe
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
for cmd in gcloud kubectl helm docker mvn; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' is required but not found. Please install it and retry."
    exit 1
  fi
done

for var in GKE_CLUSTER GKE_REGION GCP_PROJECT REGISTRY ANTHROPIC_API_KEY; do
  if [ -z "${!var:-}" ]; then
    echo "ERROR: Environment variable $var is not set."
    exit 1
  fi
done

echo "Cluster : $GKE_CLUSTER"
echo "Region  : $GKE_REGION"
echo "Project : $GCP_PROJECT"
echo "Registry: $REGISTRY"
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
# 2. Authenticate Docker to Artifact Registry
# -------------------------------------------------------
echo "--- Step 2: Configuring Docker for Artifact Registry ---"
# Extract the hostname portion of the registry (e.g., us-central1-docker.pkg.dev)
REGISTRY_HOSTNAME="$(echo "$REGISTRY" | cut -d'/' -f1)"
gcloud auth configure-docker "$REGISTRY_HOSTNAME" --quiet
echo ""

# -------------------------------------------------------
# 3. Clone and build a2a-java dependency
# -------------------------------------------------------
echo "--- Step 3: Building a2a-java dependency ---"
A2A_JAVA_DIR="$PROJECT_ROOT/.deps/a2a-java"
if [ -d "$A2A_JAVA_DIR" ]; then
  echo "a2a-java already cloned, pulling latest changes..."
  (cd "$A2A_JAVA_DIR" && git pull)
else
  mkdir -p "$PROJECT_ROOT/.deps"
  git clone https://github.com/salaboy/a2a-java "$A2A_JAVA_DIR"
fi
(cd "$A2A_JAVA_DIR" && mvn clean install -DskipTests -q)
echo ""

# -------------------------------------------------------
# 4. Clone and build langchain4j-patched dependency
# -------------------------------------------------------
echo "--- Step 4: Building langchain4j dependency ---"
LANGCHAIN4J_DIR="$PROJECT_ROOT/.deps/langchain4j"
if [ -d "$LANGCHAIN4J_DIR" ]; then
  echo "langchain4j already cloned, pulling latest changes..."
  (cd "$LANGCHAIN4J_DIR" && git pull)
else
  mkdir -p "$PROJECT_ROOT/.deps"
  git clone -b 1.11.0-beta19-patched https://github.com/salaboy/langchain4j "$LANGCHAIN4J_DIR"
fi
(cd "$LANGCHAIN4J_DIR" && mvn clean install -DskipTests -q)
echo ""

# -------------------------------------------------------
# 5. Build agent services (Maven)
# -------------------------------------------------------
echo "--- Step 5: Building agent services with Maven ---"
for agent in pizza-mcp cooking-agent delivery-agent store-mgmt-agent; do
  echo "Building agents/$agent ..."
  (cd "$PROJECT_ROOT/agents/$agent" && ./mvnw clean package -DskipTests -q)
done
echo ""

# -------------------------------------------------------
# 6. Build Docker images and push to Artifact Registry
# -------------------------------------------------------
echo "--- Step 6: Building and pushing Docker images to $REGISTRY ---"
cd "$PROJECT_ROOT"

docker build -t "$REGISTRY/pizza-vibe-store:latest"          -f store/Dockerfile .
docker build -t "$REGISTRY/pizza-vibe-inventory:latest"      -f inventory/Dockerfile .
docker build -t "$REGISTRY/pizza-vibe-oven:latest"           -f oven/Dockerfile .
docker build -t "$REGISTRY/pizza-vibe-bikes:latest"          -f bikes/Dockerfile .
docker build -t "$REGISTRY/pizza-vibe-drinks-stock:latest"   -f drinks-stock/Dockerfile .
docker build -t "$REGISTRY/pizza-vibe-pizza-mcp:latest"      -f agents/pizza-mcp/src/main/docker/Dockerfile.jvm      ./agents/pizza-mcp
docker build -t "$REGISTRY/pizza-vibe-cooking-agent:latest"  -f agents/cooking-agent/src/main/docker/Dockerfile.jvm  ./agents/cooking-agent
docker build -t "$REGISTRY/pizza-vibe-delivery-agent:latest" -f agents/delivery-agent/src/main/docker/Dockerfile.jvm .
docker build -t "$REGISTRY/pizza-vibe-store-mgmt-agent:latest" -f agents/store-mgmt-agent/src/main/docker/Dockerfile.jvm ./agents/store-mgmt-agent

for image in \
  pizza-vibe-store \
  pizza-vibe-inventory \
  pizza-vibe-oven \
  pizza-vibe-bikes \
  pizza-vibe-drinks-stock \
  pizza-vibe-pizza-mcp \
  pizza-vibe-cooking-agent \
  pizza-vibe-delivery-agent \
  pizza-vibe-store-mgmt-agent; do
  echo "Pushing $REGISTRY/$image:latest ..."
  docker push "$REGISTRY/$image:latest"
done
echo ""

# -------------------------------------------------------
# 7. Install PostgreSQL
# -------------------------------------------------------
echo "--- Step 7: Installing PostgreSQL ---"
helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update
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
# 8. Create secrets
# -------------------------------------------------------
echo "--- Step 8: Creating secrets ---"
kubectl create secret generic anthropic-secret \
  --from-literal=api-key="$ANTHROPIC_API_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Secret 'anthropic-secret' applied."
echo ""

# -------------------------------------------------------
# 9. Install Observability stack
# -------------------------------------------------------
echo "--- Step 9: Installing Observability stack ---"

# Jaeger (all-in-one, memory storage)
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts 2>/dev/null || true
helm repo update
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
  echo "Dash0 secrets applied. Collector will export to Jaeger + Dash0."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config.yaml"
else
  echo "DASH0_AUTH_TOKEN not set. Collector will export to Jaeger only."
  COLLECTOR_VALUES="$OBSERVABILITY_DIR/collector-config-jaeger-only.yaml"
fi

helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts 2>/dev/null || true
helm repo update

if helm status otel-collector -n opentelemetry &>/dev/null; then
  helm upgrade otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry -f "$COLLECTOR_VALUES" --wait
else
  helm install otel-collector open-telemetry/opentelemetry-collector \
    --namespace opentelemetry -f "$COLLECTOR_VALUES" --wait
fi
kubectl get pods -n opentelemetry -l app.kubernetes.io/name=opentelemetry-collector
echo ""

# cert-manager (required by OTel Operator)
helm repo add jetstack https://charts.jetstack.io --force-update
helm repo update
if helm status cert-manager -n cert-manager &>/dev/null; then
  echo "cert-manager is already installed, skipping."
else
  helm upgrade --install cert-manager jetstack/cert-manager \
    --namespace cert-manager --create-namespace \
    --set crds.enabled=true \
    --wait
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
echo ""

kubectl apply -f "$OBSERVABILITY_DIR/instrumentation.yaml"
echo "OTel Instrumentation resource applied."
echo ""

# -------------------------------------------------------
# 10. Deploy application services
#     Patch bare image names with the full registry path.
# -------------------------------------------------------
echo "--- Step 10: Deploying application services ---"
TMP_K8S_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_K8S_DIR"' EXIT

for f in "$PROJECT_ROOT/k8s/"*.yaml; do
  sed "s|image: pizza-vibe-|image: $REGISTRY/pizza-vibe-|g" "$f" \
    > "$TMP_K8S_DIR/$(basename "$f")"
done

kubectl apply -f "$TMP_K8S_DIR/"
echo "Restarting deployments to pull fresh images..."
kubectl rollout restart deployment
echo ""

echo "Waiting for rollout to complete..."
kubectl rollout status deployment --timeout=180s || true
kubectl get pods
echo ""

# -------------------------------------------------------
# 11. Expose the store service via LoadBalancer
# -------------------------------------------------------
echo "--- Step 11: Exposing store service via LoadBalancer ---"
kubectl patch svc store -p '{"spec":{"type":"LoadBalancer"}}'

echo "Waiting for the store LoadBalancer to receive an external IP (this can take ~60s)..."
STORE_IP=""
for i in $(seq 1 18); do
  STORE_IP="$(kubectl get svc store -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  [ -n "$STORE_IP" ] && break
  echo "  ... attempt $i/18, retrying in 10s"
  sleep 10
done

echo ""
echo "=== Setup complete ==="
echo ""
if [ -n "$STORE_IP" ]; then
  echo "Store UI:   http://$STORE_IP:8080"
else
  echo "Store external IP not yet assigned. Check with:"
  echo "  kubectl get svc store"
  echo "Then open: http://<EXTERNAL-IP>:8080"
fi
echo ""
echo "Jaeger UI (port-forward):"
echo "  kubectl port-forward svc/jaeger-query 16686"
echo "  http://localhost:16686"
echo ""
echo "PostgreSQL (port-forward):"
echo "  kubectl port-forward svc/postgresql 5432:5432"
echo "  psql postgres://postgres:postgres@localhost:5432/store-db"
