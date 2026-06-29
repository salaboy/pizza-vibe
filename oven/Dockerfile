# Build stage
FROM golang:1.25-alpine AS builder

WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the oven service
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/oven ./oven/cmd/

# Runtime stage
FROM alpine:latest

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/bin/oven .

# Expose port
EXPOSE 8085

# Run the service
CMD ["./oven"]
