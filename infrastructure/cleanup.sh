#!/bin/bash

# Infrastructure Script: Environment Cleanup
# Purpose: Safely stops and removes containers, networks, and volumes for testing/debugging
# Usage: ./cleanup.sh [--force] [--volumes] [--network]
# Options: --force (skip confirmations), --volumes (remove data), --network (remove network)
# Example: ./cleanup.sh --force --volumes

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-nginx-net}
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}
APP_CONTAINER_NAME=${APP_CONTAINER_NAME:-spring-boot-app}
POSTGRES_DATA_VOLUME=${POSTGRES_DATA_VOLUME:-postgres-data}
POSTGRES_LOG_VOLUME=${POSTGRES_LOG_VOLUME:-postgres-logs}

# Parse command line arguments
FORCE_CLEANUP=false
REMOVE_VOLUMES=false
REMOVE_NETWORK=false

for arg in "$@"; do
    case $arg in
        --force)
            FORCE_CLEANUP=true
            shift
            ;;
        --volumes)
            REMOVE_VOLUMES=true
            shift
            ;;
        --network)
            REMOVE_NETWORK=true
            shift
            ;;
        *)
            echo "Unknown option: $arg"
            echo "Usage: $0 [--force] [--volumes] [--network]"
            exit 1
            ;;
    esac
done

echo "=== Infrastructure Cleanup Script ==="
echo "Application Container: $APP_CONTAINER_NAME"
echo "Database Container: $POSTGRES_CONTAINER_NAME"
echo "Network: $DOCKER_NETWORK_NAME"

if [ "$REMOVE_VOLUMES" = true ]; then
    echo "Data Volumes: $POSTGRES_DATA_VOLUME, $POSTGRES_LOG_VOLUME (WILL BE REMOVED)"
fi

# Function to print status with color
print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS")
            echo -e "\e[32m✓ $message\e[0m"
            ;;
        "WARNING")
            echo -e "\e[33m⚠ $message\e[0m"
            ;;
        "INFO")
            echo -e "\e[34mℹ $message\e[0m"
            ;;
        "ERROR")
            echo -e "\e[31m✗ $message\e[0m"
            ;;
    esac
}

# Confirmation prompt
if [ "$FORCE_CLEANUP" = false ]; then
    echo ""
    echo "This will stop and remove:"
    echo "  - Application container ($APP_CONTAINER_NAME)"
    echo "  - Database container ($POSTGRES_CONTAINER_NAME)"
    
    if [ "$REMOVE_VOLUMES" = true ]; then
        echo "  - Database volumes (ALL DATA WILL BE LOST!)"
    fi
    
    if [ "$REMOVE_NETWORK" = true ]; then
        echo "  - Docker network ($DOCKER_NETWORK_NAME)"
    fi
    
    echo ""
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cleanup cancelled"
        exit 0
    fi
fi

# Check prerequisites
if ! command -v docker &> /dev/null; then
    print_status "ERROR" "Docker is not installed or not in PATH"
    exit 1
fi

# 1. Stop and remove application container
echo ""
echo "=== Step 1: Application Container Cleanup ==="
if docker ps | grep -q "$APP_CONTAINER_NAME"; then
    print_status "INFO" "Stopping application container..."
    docker stop "$APP_CONTAINER_NAME"
    print_status "SUCCESS" "Application container stopped"
else
    print_status "INFO" "Application container is not running"
fi

if docker ps -a | grep -q "$APP_CONTAINER_NAME"; then
    print_status "INFO" "Removing application container..."
    docker rm "$APP_CONTAINER_NAME"
    print_status "SUCCESS" "Application container removed"
else
    print_status "INFO" "Application container does not exist"
fi

# 2. Stop and remove database container
echo ""
echo "=== Step 2: Database Container Cleanup ==="
if docker ps | grep -q "$POSTGRES_CONTAINER_NAME"; then
    print_status "INFO" "Stopping database container..."
    docker stop "$POSTGRES_CONTAINER_NAME"
    print_status "SUCCESS" "Database container stopped"
else
    print_status "INFO" "Database container is not running"
fi

if docker ps -a | grep -q "$POSTGRES_CONTAINER_NAME"; then
    print_status "INFO" "Removing database container..."
    docker rm "$POSTGRES_CONTAINER_NAME"
    print_status "SUCCESS" "Database container removed"
else
    print_status "INFO" "Database container does not exist"
fi

# 3. Remove Docker volumes (if requested)
if [ "$REMOVE_VOLUMES" = true ]; then
    echo ""
    echo "=== Step 3: Volume Cleanup ==="
    
    if [ "$FORCE_CLEANUP" = false ]; then
        echo ""
        print_status "WARNING" "This will permanently delete ALL database data!"
        read -p "Type 'DELETE' to confirm volume removal: " -r
        echo
        
        if [ "$REPLY" != "DELETE" ]; then
            print_status "INFO" "Volume removal cancelled"
        else
            # Remove volumes
            for volume in "$POSTGRES_DATA_VOLUME" "$POSTGRES_LOG_VOLUME"; do
                if docker volume ls | grep -q "$volume"; then
                    print_status "INFO" "Removing volume: $volume"
                    docker volume rm "$volume"
                    print_status "SUCCESS" "Volume removed: $volume"
                else
                    print_status "INFO" "Volume does not exist: $volume"
                fi
            done
        fi
    else
        # Force removal
        for volume in "$POSTGRES_DATA_VOLUME" "$POSTGRES_LOG_VOLUME"; do
            if docker volume ls | grep -q "$volume"; then
                print_status "INFO" "Removing volume: $volume"
                docker volume rm "$volume"
                print_status "SUCCESS" "Volume removed: $volume"
            else
                print_status "INFO" "Volume does not exist: $volume"
            fi
        done
    fi
else
    print_status "INFO" "Keeping database volumes (use --volumes to remove)"
fi

# 4. Remove Docker images (optional)
echo ""
echo "=== Step 4: Image Cleanup (Optional) ==="
if docker images | grep -q "$APP_CONTAINER_NAME"; then
    if [ "$FORCE_CLEANUP" = false ]; then
        read -p "Remove application Docker image? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker rmi "$APP_CONTAINER_NAME:latest" || print_status "WARNING" "Could not remove application image"
            print_status "SUCCESS" "Application image removed"
        fi
    else
        docker rmi "$APP_CONTAINER_NAME:latest" || print_status "WARNING" "Could not remove application image"
        print_status "SUCCESS" "Application image removed"
    fi
else
    print_status "INFO" "No application image to remove"
fi

# 5. Remove Docker network (if requested)
if [ "$REMOVE_NETWORK" = true ]; then
    echo ""
    echo "=== Step 5: Network Cleanup ==="
    
    if docker network ls | grep -q "$DOCKER_NETWORK_NAME"; then
        print_status "INFO" "Removing Docker network: $DOCKER_NETWORK_NAME"
        docker network rm "$DOCKER_NETWORK_NAME"
        print_status "SUCCESS" "Network removed: $DOCKER_NETWORK_NAME"
    else
        print_status "INFO" "Network does not exist: $DOCKER_NETWORK_NAME"
    fi
else
    print_status "INFO" "Keeping Docker network (use --network to remove)"
fi

# 6. Clean up dangling resources
echo ""
echo "=== Step 6: System Cleanup ==="
print_status "INFO" "Cleaning up dangling Docker resources..."

# Remove dangling images
DANGLING_IMAGES=$(docker images -f "dangling=true" -q)
if [ -n "$DANGLING_IMAGES" ]; then
    docker rmi $DANGLING_IMAGES
    print_status "SUCCESS" "Removed dangling images"
else
    print_status "INFO" "No dangling images to remove"
fi

# Show remaining resources
echo ""
echo "=== Cleanup Summary ==="
print_status "SUCCESS" "Cleanup completed successfully"

echo ""
echo "Remaining Docker resources:"
echo "Containers:"
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -10

echo ""
echo "Networks:"
docker network ls --format "table {{.Name}}\t{{.Driver}}\t{{.Scope}}" | head -10

if [ "$REMOVE_VOLUMES" = false ]; then
    echo ""
    echo "Volumes (preserved):"
    docker volume ls --format "table {{.Name}}\t{{.Driver}}" | grep -E "(postgres|chain)" || echo "No database volumes found"
fi

echo ""
print_status "INFO" "To restart infrastructure:"
echo "1. ./network-create.sh"
echo "2. ./db-start.sh"
echo "3. ./app-build-run.sh"