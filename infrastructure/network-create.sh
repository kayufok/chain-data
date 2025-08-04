#!/bin/bash

# Infrastructure Script: Docker Network Creation
# Purpose: Creates the nginx-net Docker network if it doesn't exist
# Usage: ./network-create.sh
# Required Environment Variables: DOCKER_NETWORK_NAME (optional, defaults to nginx-net)
# Example: ./network-create.sh

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-nginx-net}

echo "=== Docker Network Creation Script ==="
echo "Network Name: $DOCKER_NETWORK_NAME"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running"
    exit 1
fi

# Check if network already exists
if docker network ls | grep -q "$DOCKER_NETWORK_NAME"; then
    echo "INFO: Network '$DOCKER_NETWORK_NAME' already exists"
    echo "Network details:"
    docker network inspect "$DOCKER_NETWORK_NAME" --format '{{json .}}' | jq -r '.Name, .Driver, .Scope'
else
    echo "INFO: Creating Docker network '$DOCKER_NETWORK_NAME'"
    docker network create "$DOCKER_NETWORK_NAME"
    echo "SUCCESS: Network '$DOCKER_NETWORK_NAME' created successfully"
fi

# Verify network creation
echo "=== Network Verification ==="
docker network ls | grep "$DOCKER_NETWORK_NAME"
echo "Network '$DOCKER_NETWORK_NAME' is ready for use"