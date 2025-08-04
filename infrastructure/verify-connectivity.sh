#!/bin/bash

# Infrastructure Script: Connectivity Verification
# Purpose: Verifies that all containers are running and Spring Boot app connects to PostgreSQL
# Usage: ./verify-connectivity.sh
# Required Environment Variables: See .env.example
# Example: ./verify-connectivity.sh

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
DOCKER_NETWORK_NAME=${DOCKER_NETWORK_NAME:-nginx-net}
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}
APP_CONTAINER_NAME=${APP_CONTAINER_NAME:-spring-boot-app}
APP_PORT=${APP_PORT:-8080}
POSTGRES_DB=${POSTGRES_DB:-chaindata}
POSTGRES_USER=${POSTGRES_USER:-chainuser}

echo "=== Infrastructure Connectivity Verification ==="
echo "Network: $DOCKER_NETWORK_NAME"
echo "Database Container: $POSTGRES_CONTAINER_NAME"
echo "Application Container: $APP_CONTAINER_NAME"
echo "Application Port: $APP_PORT"

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running"
    exit 1
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
        "ERROR")
            echo -e "\e[31m✗ $message\e[0m"
            ;;
        "INFO")
            echo -e "\e[34mℹ $message\e[0m"
            ;;
    esac
}

# 1. Verify Docker Network
echo ""
echo "=== Step 1: Docker Network Verification ==="
if docker network ls | grep -q "$DOCKER_NETWORK_NAME"; then
    print_status "SUCCESS" "Docker network '$DOCKER_NETWORK_NAME' exists"
    
    # Show network details
    NETWORK_INFO=$(docker network inspect "$DOCKER_NETWORK_NAME" --format '{{.Driver}} {{.Scope}}')
    print_status "INFO" "Network details: $NETWORK_INFO"
else
    print_status "ERROR" "Docker network '$DOCKER_NETWORK_NAME' does not exist"
    echo "Run: ./network-create.sh"
    exit 1
fi

# 2. Verify PostgreSQL Container
echo ""
echo "=== Step 2: PostgreSQL Container Verification ==="
if docker ps | grep -q "$POSTGRES_CONTAINER_NAME"; then
    print_status "SUCCESS" "PostgreSQL container '$POSTGRES_CONTAINER_NAME' is running"
    
    # Check if container is on the correct network
    if docker inspect "$POSTGRES_CONTAINER_NAME" --format '{{json .NetworkSettings.Networks}}' | grep -q "$DOCKER_NETWORK_NAME"; then
        print_status "SUCCESS" "PostgreSQL container is attached to '$DOCKER_NETWORK_NAME'"
    else
        print_status "WARNING" "PostgreSQL container may not be on '$DOCKER_NETWORK_NAME'"
    fi
    
    # Test database connectivity
    print_status "INFO" "Testing PostgreSQL connectivity..."
    if docker exec "$POSTGRES_CONTAINER_NAME" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" &> /dev/null; then
        print_status "SUCCESS" "PostgreSQL is accepting connections"
    else
        print_status "ERROR" "PostgreSQL is not accepting connections"
        exit 1
    fi
else
    print_status "ERROR" "PostgreSQL container '$POSTGRES_CONTAINER_NAME' is not running"
    echo "Run: ./db-start.sh"
    exit 1
fi

# 3. Verify Spring Boot Application Container
echo ""
echo "=== Step 3: Spring Boot Application Container Verification ==="
if docker ps | grep -q "$APP_CONTAINER_NAME"; then
    print_status "SUCCESS" "Application container '$APP_CONTAINER_NAME' is running"
    
    # Check if container is on the correct network
    if docker inspect "$APP_CONTAINER_NAME" --format '{{json .NetworkSettings.Networks}}' | grep -q "$DOCKER_NETWORK_NAME"; then
        print_status "SUCCESS" "Application container is attached to '$DOCKER_NETWORK_NAME'"
    else
        print_status "WARNING" "Application container may not be on '$DOCKER_NETWORK_NAME'"
    fi
else
    print_status "ERROR" "Application container '$APP_CONTAINER_NAME' is not running"
    echo "Run: ./app-build-run.sh"
    exit 1
fi

# 4. Verify Application Health
echo ""
echo "=== Step 4: Application Health Verification ==="
print_status "INFO" "Waiting for application to be ready..."

# Wait for application to start (up to 60 seconds)
for i in {1..60}; do
    if curl -f -s http://localhost:$APP_PORT/actuator/health &> /dev/null; then
        print_status "SUCCESS" "Application health endpoint is responding"
        HEALTH_STATUS=$(curl -s http://localhost:$APP_PORT/actuator/health | grep -o '"status":"[^"]*"' || echo "unknown")
        print_status "INFO" "Health status: $HEALTH_STATUS"
        break
    elif curl -f -s http://localhost:$APP_PORT &> /dev/null; then
        print_status "SUCCESS" "Application is responding on port $APP_PORT"
        break
    elif [ $i -eq 60 ]; then
        print_status "WARNING" "Application health check timeout, checking logs..."
    else
        if [ $((i % 10)) -eq 0 ]; then
            print_status "INFO" "Waiting for application... ($i/60)"
        fi
        sleep 1
    fi
done

# 5. Verify Database Connection in Application Logs
echo ""
echo "=== Step 5: Database Connection Verification ==="
print_status "INFO" "Checking application logs for database connection..."

# Get recent application logs
APP_LOGS=$(docker logs "$APP_CONTAINER_NAME" --tail 100 2>&1)

# Check for successful database connection indicators
if echo "$APP_LOGS" | grep -qi "database\|postgres\|connection.*established\|hikari.*started\|jpa.*started"; then
    print_status "SUCCESS" "Application logs show database connection activity"
    
    # Look for specific success patterns
    if echo "$APP_LOGS" | grep -qi "started.*application\|application.*started"; then
        print_status "SUCCESS" "Spring Boot application started successfully"
    fi
    
    if echo "$APP_LOGS" | grep -qi "hikari.*started\|connection.*pool"; then
        print_status "SUCCESS" "Database connection pool initialized"
    fi
    
    if echo "$APP_LOGS" | grep -qi "jpa.*started\|hibernate.*started"; then
        print_status "SUCCESS" "JPA/Hibernate initialized successfully"
    fi
else
    print_status "WARNING" "No clear database connection indicators found in logs"
fi

# Check for error patterns
if echo "$APP_LOGS" | grep -qi "error\|exception\|failed\|cannot.*connect"; then
    print_status "WARNING" "Found error patterns in application logs"
    echo ""
    echo "Recent error logs:"
    echo "$APP_LOGS" | grep -i "error\|exception\|failed" | tail -5
fi

# 6. Network Connectivity Test
echo ""
echo "=== Step 6: Network Connectivity Test ==="
print_status "INFO" "Testing network connectivity between containers..."

# Test if app can reach database
if docker exec "$APP_CONTAINER_NAME" sh -c "command -v nc" &> /dev/null; then
    if docker exec "$APP_CONTAINER_NAME" nc -z "$POSTGRES_CONTAINER_NAME" 5432; then
        print_status "SUCCESS" "Application can reach PostgreSQL on port 5432"
    else
        print_status "ERROR" "Application cannot reach PostgreSQL"
    fi
elif docker exec "$APP_CONTAINER_NAME" sh -c "command -v curl" &> /dev/null; then
    if docker exec "$APP_CONTAINER_NAME" curl -s "$POSTGRES_CONTAINER_NAME:5432" &> /dev/null; then
        print_status "SUCCESS" "Application can reach PostgreSQL"
    else
        print_status "WARNING" "Network connectivity test inconclusive"
    fi
else
    print_status "INFO" "Network tools not available for connectivity test"
fi

# 7. Final Summary
echo ""
echo "=== VERIFICATION SUMMARY ==="

# Count running containers on the network
CONTAINERS_ON_NETWORK=$(docker network inspect "$DOCKER_NETWORK_NAME" --format '{{len .Containers}}')
print_status "INFO" "Containers on '$DOCKER_NETWORK_NAME': $CONTAINERS_ON_NETWORK"

# Final connectivity status
if docker ps | grep -q "$POSTGRES_CONTAINER_NAME" && docker ps | grep -q "$APP_CONTAINER_NAME"; then
    if echo "$APP_LOGS" | grep -qi "started.*application\|application.*started"; then
        print_status "SUCCESS" "VERIFICATION PASSED: All containers running and application started"
        echo ""
        echo "✅ Infrastructure is ready!"
        echo "🌐 Application URL: http://localhost:$APP_PORT"
        if curl -f -s http://localhost:$APP_PORT/actuator/health &> /dev/null; then
            echo "🩺 Health Check: http://localhost:$APP_PORT/actuator/health"
        fi
        echo ""
        exit 0
    else
        print_status "WARNING" "VERIFICATION PARTIAL: Containers running but application may not be fully started"
        echo ""
        echo "📋 Troubleshooting steps:"
        echo "1. Check logs: ./app-build-run.sh logs"
        echo "2. Restart app: ./app-build-run.sh restart"
        echo "3. Check database: ./db-start.sh"
        exit 2
    fi
else
    print_status "ERROR" "VERIFICATION FAILED: Required containers not running"
    echo ""
    echo "📋 Required steps:"
    echo "1. Create network: ./network-create.sh"
    echo "2. Start database: ./db-start.sh"
    echo "3. Start application: ./app-build-run.sh"
    exit 1
fi