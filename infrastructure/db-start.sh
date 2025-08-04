#!/bin/bash

# Infrastructure Script: PostgreSQL 17 Container Start
# Purpose: Creates and starts a persistent PostgreSQL 17 container on nginx-net network
# Usage: ./db-start.sh
# Required Environment Variables: See .env.example
# Example: ./db-start.sh

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-nginx-net}
POSTGRES_VERSION=${POSTGRES_VERSION:-17}
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}
POSTGRES_DB=${POSTGRES_DB:-chaindata}
POSTGRES_USER=${POSTGRES_USER:-chainuser}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-changeme123}
POSTGRES_PORT=${POSTGRES_PORT:-5432}
POSTGRES_DATA_VOLUME=${POSTGRES_DATA_VOLUME:-postgres-data}
POSTGRES_LOG_VOLUME=${POSTGRES_LOG_VOLUME:-postgres-logs}

echo "=== PostgreSQL 17 Container Start Script ==="
echo "Container Name: $POSTGRES_CONTAINER_NAME"
echo "Database: $POSTGRES_DB"
echo "User: $POSTGRES_USER"
echo "Port: $POSTGRES_PORT"
echo "Network: $DOCKER_NETWORK_NAME"

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running"
    exit 1
fi

# Check if network exists
if ! docker network ls | grep -q "$DOCKER_NETWORK_NAME"; then
    echo "ERROR: Docker network '$DOCKER_NETWORK_NAME' does not exist"
    echo "Please run ./network-create.sh first"
    exit 1
fi

# Check if container already exists
if docker ps -a | grep -q "$POSTGRES_CONTAINER_NAME"; then
    echo "INFO: Container '$POSTGRES_CONTAINER_NAME' already exists"
    
    # Check if it's running
    if docker ps | grep -q "$POSTGRES_CONTAINER_NAME"; then
        echo "INFO: Container '$POSTGRES_CONTAINER_NAME' is already running"
        docker ps | grep "$POSTGRES_CONTAINER_NAME"
        exit 0
    else
        echo "INFO: Starting existing container '$POSTGRES_CONTAINER_NAME'"
        docker start "$POSTGRES_CONTAINER_NAME"
    fi
else
    echo "INFO: Creating PostgreSQL container with persistent volumes"
    
    # Create volumes if they don't exist
    docker volume create "$POSTGRES_DATA_VOLUME" || true
    docker volume create "$POSTGRES_LOG_VOLUME" || true
    
    # Create and start the container
    docker run -d \
        --name "$POSTGRES_CONTAINER_NAME" \
        --network "$DOCKER_NETWORK_NAME" \
        -e POSTGRES_DB="$POSTGRES_DB" \
        -e POSTGRES_USER="$POSTGRES_USER" \
        -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
        -v "$POSTGRES_DATA_VOLUME:/var/lib/postgresql/data" \
        -v "$POSTGRES_LOG_VOLUME:/var/log/postgresql" \
        -p "$POSTGRES_PORT:5432" \
        --restart unless-stopped \
        postgres:"$POSTGRES_VERSION"
    
    echo "SUCCESS: PostgreSQL container '$POSTGRES_CONTAINER_NAME' created and started"
fi

# Wait for PostgreSQL to be ready
echo "INFO: Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec "$POSTGRES_CONTAINER_NAME" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" &> /dev/null; then
        echo "SUCCESS: PostgreSQL is ready and accepting connections"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Verify container status
echo "=== Container Status ==="
docker ps | grep "$POSTGRES_CONTAINER_NAME"
echo "PostgreSQL container is running and ready for connections"