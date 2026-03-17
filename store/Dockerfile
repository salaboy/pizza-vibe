# Stage 1: Build the front-end static export
FROM node:20-slim AS node-builder

WORKDIR /app

# Copy front-end sources
COPY front-end/package.json front-end/package-lock.json ./
RUN npm ci

COPY front-end/ .

# Override next.config.ts to use 'export' instead of 'standalone'
RUN sed -i "s/output: 'standalone'/output: 'export'/" next.config.ts

# Remove API routes (incompatible with static export; Go store serves /api/* instead)
RUN rm -rf src/app/api

RUN npm run build
# Produces /app/out/

# Stage 2: Build the Go store binary
FROM golang:1.24-alpine AS go-builder

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the store service
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/store ./store/cmd/

# Stage 3: Runtime
FROM alpine:latest

WORKDIR /app

# Copy the Go binary
COPY --from=go-builder /app/bin/store .

# Copy the front-end static export
COPY --from=node-builder /app/out/ ./static/

# Expose port
EXPOSE 8080

# Run the service
CMD ["./store"]
