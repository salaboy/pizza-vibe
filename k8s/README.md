# Pizza Vibe - Kubernetes Installation Guide

This guide walks you through deploying Pizza Vibe into a local Kubernetes cluster using [KIND](https://kind.sigs.k8s.io/).

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [KIND](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- [Helm](https://helm.sh/docs/intro/install/)
- [Java 17+](https://adoptium.net/) (for building agent services)
- [Maven](https://maven.apache.org/install.html) (or use the included Maven wrapper)

## Quick Start (Script)

> **Warning:** The `ANTHROPIC_API_KEY` environment variable **must** be set before running the setup script. The script will exit immediately if it is missing. The agent services (cooking-agent, delivery-agent, store-mgmt-agent) need this key to call the Anthropic API.

```bash
export ANTHROPIC_API_KEY=<YOUR_ANTHROPIC_API_KEY>
./scripts/setup-kind.sh
```

The script is idempotent — it skips steps that are already completed (existing cluster, Dapr already installed, etc.).

## 1. Create a KIND Cluster

```bash
kind create cluster --name pizza-vibe
```

Verify the cluster is running:

```bash
kubectl cluster-info --context kind-pizza-vibe
```

## 2. Install Dapr

The `store-mgmt-agent` service relies on Dapr for workflow orchestration. Install Dapr using Helm:

```bash
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm install dapr dapr/dapr --namespace dapr-system --create-namespace --wait
```

Verify Dapr is running:

```bash
kubectl get pods -n dapr-system
```

You should see `dapr-operator`, `dapr-sidecar-injector`, `dapr-sentry`, and `dapr-placement-server` pods in a `Running` state.

## 3. Install PostgreSQL

The `store-mgmt-agent` uses Dapr workflows which require an actor state store backed by PostgreSQL. Install it using Helm:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install postgresql bitnami/postgresql \
  --set auth.postgresPassword=postgres \
  --set auth.database=dapr_store \
  --wait
```

Verify PostgreSQL is running:

```bash
kubectl get pods -l app.kubernetes.io/name=postgresql
```

## 4. Clone and Build `quarkus-agentic-dapr`

The `store-mgmt-agent` depends on the [quarkus-agentic-dapr](https://github.com/salaboy/quarkus-agentic-dapr) library which is not published to a public Maven repository. You must clone and install it locally before building the agents:

```bash
git clone https://github.com/salaboy/quarkus-agentic-dapr .deps/quarkus-agentic-dapr
cd .deps/quarkus-agentic-dapr && mvn clean install -DskipTests && cd ../..
```

If you have already cloned it, pull the latest changes and rebuild:

```bash
cd .deps/quarkus-agentic-dapr && git pull && mvn clean install -DskipTests && cd ../..
```

## 5. Build the Agent Services (Maven)

The Java/Quarkus agent services under `agents/` must be packaged before building their Docker images. From the project root, run:

```bash
cd agents/pizza-mcp && ./mvnw clean package -DskipTests && cd ../..
cd agents/cooking-agent && ./mvnw clean package -DskipTests && cd ../..
cd agents/delivery-agent && ./mvnw clean package -DskipTests && cd ../..
cd agents/store-mgmt-agent && ./mvnw clean package -DskipTests && cd ../..
```

## 6. Build and Load Container Images

Since KIND runs containers inside Docker, you need to build the images locally and then load them into the KIND cluster.

Build all images from the project root:

```bash
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
```

Load all images into the KIND cluster:

```bash
kind load docker-image pizza-vibe-store:latest --name pizza-vibe
kind load docker-image pizza-vibe-inventory:latest --name pizza-vibe
kind load docker-image pizza-vibe-oven:latest --name pizza-vibe
kind load docker-image pizza-vibe-bikes:latest --name pizza-vibe
kind load docker-image pizza-vibe-drinks-stock:latest --name pizza-vibe
kind load docker-image pizza-vibe-pizza-mcp:latest --name pizza-vibe
kind load docker-image pizza-vibe-cooking-agent:latest --name pizza-vibe
kind load docker-image pizza-vibe-delivery-agent:latest --name pizza-vibe
kind load docker-image pizza-vibe-store-mgmt-agent:latest --name pizza-vibe
```

## 7. Create Secrets

The `delivery-agent` requires an Anthropic API key. Create the secret before deploying:

```bash
kubectl create secret generic anthropic-secret --from-literal=api-key=<YOUR_ANTHROPIC_API_KEY>
```

## 8. Deploy the Application

Apply all Kubernetes manifests:

```bash
kubectl apply -f k8s/
```

Verify all pods are running:

```bash
kubectl get pods
```

## 9. Access the Application

Port-forward the store service to access it from your browser:

```bash
kubectl port-forward svc/store 8080:8080
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

The store serves the front-end static files, API endpoints, and WebSocket connections all on port 8080 — no additional port-forwarding is needed.

### Accessing PostgreSQL from outside the cluster

To connect to the in-cluster PostgreSQL instance from your local machine (e.g. with `psql` or a GUI client), port-forward the PostgreSQL service:

```bash
kubectl port-forward svc/postgresql 5432:5432
```

Then connect using:

```bash
psql postgres://postgres:postgres@localhost:5432/dapr_store
```

## Services

| Service          | Port | Description                              |
|------------------|------|------------------------------------------|
| store            | 8080 | Store API + front-end static files       |
| inventory        | 8084 | Ingredient inventory service             |
| oven             | 8085 | Oven management service                  |
| pizza-mcp        | 8086 | MCP server for pizza tools (Quarkus)     |
| cooking-agent    | 8087 | AI-powered cooking agent (Quarkus)       |
| bikes            | 8088 | Delivery bikes service                   |
| delivery-agent   | 8089 | AI-powered delivery agent (Quarkus)      |
| drinks-stock     | 8090 | Drinks stock management service          |
| store-mgmt-agent | 9999 | Store management agent with Dapr (Quarkus) |

## Cleanup

Delete the KIND cluster:

```bash
kind delete cluster --name pizza-vibe
```
