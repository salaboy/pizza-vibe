# Build stage
FROM golang:1.24-alpine AS builder

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the drinks-stock service
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/drinks-stock ./drinks-stock/cmd/

# Runtime stage
FROM alpine:latest

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/bin/drinks-stock .

# Expose port
EXPOSE 8090

# Run the service
CMD ["./drinks-stock"]
