#!/bin/bash

# Infrastructure Script: Spring Boot Application Build and Run
# Purpose: Builds and runs the Java Spring Boot application container on nginx-net
# Usage: ./app-build-run.sh [action]
# Actions: build, run, stop, restart, logs, status
# Required Environment Variables: See .env.example
# Example: ./app-build-run.sh build

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-nginx-net}
APP_CONTAINER_NAME=${APP_CONTAINER_NAME:-spring-boot-app}
APP_PORT=${APP_PORT:-8080}
APP_JAR_NAME=${APP_JAR_NAME:-chain-data-0.0.1-SNAPSHOT.jar}
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}

# Determine action
ACTION=${1:-run}

echo "=== Spring Boot Application Management Script ==="
echo "Action: $ACTION"
echo "Container Name: $APP_CONTAINER_NAME"
echo "Port: $APP_PORT"
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

# Function to build the application
build_app() {
    echo "INFO: Building Spring Boot application..."
    
    # Check if gradlew exists
    if [ ! -f "../gradlew" ]; then
        echo "ERROR: gradlew not found. Please run from infrastructure directory"
        exit 1
    fi
    
    # Build the application
    cd ..
    ./gradlew clean build -x test
    cd infrastructure
    
    # Check if JAR file was created
    if [ ! -f "../build/libs/$APP_JAR_NAME" ]; then
        echo "ERROR: JAR file not found at ../build/libs/$APP_JAR_NAME"
        exit 1
    fi
    
    echo "SUCCESS: Application built successfully"
    echo "JAR location: ../build/libs/$APP_JAR_NAME"
    
    # Build Docker image
    echo "INFO: Building Docker image..."
    docker build -t "$APP_CONTAINER_NAME:latest" -f Dockerfile ..
    
    echo "SUCCESS: Docker image built successfully"
}

# Function to run the application
run_app() {
    # Check if network exists
    if ! docker network ls | grep -q "$DOCKER_NETWORK_NAME"; then
        echo "ERROR: Docker network '$DOCKER_NETWORK_NAME' does not exist"
        echo "Please run ./network-create.sh first"
        exit 1
    fi
    
    # Check if PostgreSQL is running
    if ! docker ps | grep -q "$POSTGRES_CONTAINER_NAME"; then
        echo "WARNING: PostgreSQL container '$POSTGRES_CONTAINER_NAME' is not running"
        echo "Please run ./db-start.sh first"
        read -p "Do you want to continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # Stop existing container if running
    if docker ps | grep -q "$APP_CONTAINER_NAME"; then
        echo "INFO: Stopping existing container..."
        docker stop "$APP_CONTAINER_NAME"
    fi
    
    # Remove existing container
    if docker ps -a | grep -q "$APP_CONTAINER_NAME"; then
        echo "INFO: Removing existing container..."
        docker rm "$APP_CONTAINER_NAME"
    fi
    
    # Run the container
    echo "INFO: Starting Spring Boot application container..."
    docker run -d \
        --name "$APP_CONTAINER_NAME" \
        --network "$DOCKER_NETWORK_NAME" \
        -p "$APP_PORT:8080" \
        -e SPRING_DATASOURCE_URL="jdbc:postgresql://$POSTGRES_CONTAINER_NAME:5432/chaindata" \
        -e SPRING_DATASOURCE_USERNAME="chainuser" \
        -e SPRING_DATASOURCE_PASSWORD="changeme123" \
        -e SPRING_JPA_HIBERNATE_DDL_AUTO="update" \
        --restart unless-stopped \
        "$APP_CONTAINER_NAME:latest"
    
    echo "SUCCESS: Application container started"
    
    # Wait for application to be ready
    echo "INFO: Waiting for application to be ready..."
    for i in {1..60}; do
        if curl -f http://localhost:$APP_PORT/actuator/health &> /dev/null; then
            echo "SUCCESS: Application is ready and healthy"
            break
        elif [ $i -eq 60 ]; then
            echo "WARNING: Application health check timeout"
            echo "Check logs with: $0 logs"
        else
            echo "Waiting... ($i/60)"
            sleep 2
        fi
    done
}

# Function to stop the application
stop_app() {
    if docker ps | grep -q "$APP_CONTAINER_NAME"; then
        echo "INFO: Stopping application container..."
        docker stop "$APP_CONTAINER_NAME"
        echo "SUCCESS: Application stopped"
    else
        echo "INFO: Application container is not running"
    fi
}

# Function to show application logs
show_logs() {
    if docker ps -a | grep -q "$APP_CONTAINER_NAME"; then
        echo "=== Application Logs ==="
        docker logs --tail 50 -f "$APP_CONTAINER_NAME"
    else
        echo "ERROR: Application container does not exist"
        exit 1
    fi
}

# Function to show application status
show_status() {
    echo "=== Application Status ==="
    
    if docker ps | grep -q "$APP_CONTAINER_NAME"; then
        echo "Container Status: RUNNING"
        docker ps | grep "$APP_CONTAINER_NAME"
        
        echo ""
        echo "Health Check:"
        if curl -f http://localhost:$APP_PORT/actuator/health 2>/dev/null; then
            echo "Application is healthy"
        else
            echo "Application health check failed"
        fi
    elif docker ps -a | grep -q "$APP_CONTAINER_NAME"; then
        echo "Container Status: STOPPED"
        docker ps -a | grep "$APP_CONTAINER_NAME"
    else
        echo "Container Status: NOT CREATED"
    fi
}

# Execute action
case $ACTION in
    build)
        build_app
        ;;
    run)
        build_app
        run_app
        show_status
        ;;
    stop)
        stop_app
        ;;
    restart)
        stop_app
        sleep 2
        run_app
        show_status
        ;;
    logs)
        show_logs
        ;;
    status)
        show_status
        ;;
    *)
        echo "ERROR: Unknown action '$ACTION'"
        echo "Available actions: build, run, stop, restart, logs, status"
        exit 1
        ;;
esac