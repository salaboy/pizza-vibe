# Build stage
FROM golang:1.25-alpine AS builder

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the inventory service
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/inventory ./inventory/cmd/

# Runtime stage
FROM alpine:latest

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/bin/inventory .

# Expose port
EXPOSE 8084

# Run the service
CMD ["./inventory"]
