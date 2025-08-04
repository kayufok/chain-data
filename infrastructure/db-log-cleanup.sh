#!/bin/bash

# Infrastructure Script: PostgreSQL Log Cleanup
# Purpose: Cleans up PostgreSQL logs based on retention policy
# Usage: ./db-log-cleanup.sh [retention_days] [max_size]
# Required Environment Variables: See .env.example
# Example: ./db-log-cleanup.sh 7 100M

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}
LOG_RETENTION_DAYS=${1:-${LOG_RETENTION_DAYS:-7}}
LOG_MAX_SIZE=${2:-${LOG_MAX_SIZE:-100M}}
POSTGRES_LOG_VOLUME=${POSTGRES_LOG_VOLUME:-postgres-logs}

echo "=== PostgreSQL Log Cleanup Script ==="
echo "Container: $POSTGRES_CONTAINER_NAME"
echo "Log Retention: $LOG_RETENTION_DAYS days"
echo "Max Log Size: $LOG_MAX_SIZE"
echo "Log Volume: $POSTGRES_LOG_VOLUME"

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

# Check if container exists
if ! docker ps -a | grep -q "$POSTGRES_CONTAINER_NAME"; then
    echo "WARNING: PostgreSQL container '$POSTGRES_CONTAINER_NAME' does not exist"
    echo "Log cleanup skipped"
    exit 0
fi

# Function to convert size to bytes
size_to_bytes() {
    local size=$1
    local unit=${size: -1}
    local number=${size%?}
    
    case $unit in
        k|K) echo $((number * 1024)) ;;
        m|M) echo $((number * 1024 * 1024)) ;;
        g|G) echo $((number * 1024 * 1024 * 1024)) ;;
        *) echo $number ;;
    esac
}

# Clean up old log files by age
echo "INFO: Cleaning up log files older than $LOG_RETENTION_DAYS days..."

# Get log directory from container
LOG_DIR="/var/log/postgresql"

# Count log files before cleanup
INITIAL_COUNT=$(docker exec "$POSTGRES_CONTAINER_NAME" find "$LOG_DIR" -name "*.log" -type f 2>/dev/null | wc -l || echo "0")
echo "Initial log file count: $INITIAL_COUNT"

# Remove old log files
REMOVED_BY_AGE=$(docker exec "$POSTGRES_CONTAINER_NAME" find "$LOG_DIR" -name "*.log" -type f -mtime +$LOG_RETENTION_DAYS -delete -print 2>/dev/null | wc -l || echo "0")
echo "Removed $REMOVED_BY_AGE log files older than $LOG_RETENTION_DAYS days"

# Clean up large log files (rotate them)
echo "INFO: Checking for log files larger than $LOG_MAX_SIZE..."
MAX_SIZE_BYTES=$(size_to_bytes "$LOG_MAX_SIZE")

# Find and rotate large log files
LARGE_FILES=$(docker exec "$POSTGRES_CONTAINER_NAME" find "$LOG_DIR" -name "*.log" -type f -size +$LOG_MAX_SIZE 2>/dev/null || echo "")

if [ -n "$LARGE_FILES" ]; then
    echo "Found large log files:"
    echo "$LARGE_FILES"
    
    # Rotate large files
    ROTATED_COUNT=0
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            TIMESTAMP=$(date +%Y%m%d-%H%M%S)
            ROTATED_FILE="${file}.${TIMESTAMP}"
            
            echo "Rotating: $file -> $ROTATED_FILE"
            docker exec "$POSTGRES_CONTAINER_NAME" mv "$file" "$ROTATED_FILE"
            
            # Create new empty log file with proper permissions
            docker exec "$POSTGRES_CONTAINER_NAME" touch "$file"
            docker exec "$POSTGRES_CONTAINER_NAME" chown postgres:postgres "$file"
            docker exec "$POSTGRES_CONTAINER_NAME" chmod 600 "$file"
            
            ((ROTATED_COUNT++))
        fi
    done <<< "$LARGE_FILES"
    
    echo "Rotated $ROTATED_COUNT large log files"
else
    echo "No log files larger than $LOG_MAX_SIZE found"
fi

# Clean up rotated log files older than retention period
REMOVED_ROTATED=$(docker exec "$POSTGRES_CONTAINER_NAME" find "$LOG_DIR" -name "*.log.*" -type f -mtime +$LOG_RETENTION_DAYS -delete -print 2>/dev/null | wc -l || echo "0")
echo "Removed $REMOVED_ROTATED old rotated log files"

# Final log file count and size summary
FINAL_COUNT=$(docker exec "$POSTGRES_CONTAINER_NAME" find "$LOG_DIR" -name "*.log*" -type f 2>/dev/null | wc -l || echo "0")
TOTAL_SIZE=$(docker exec "$POSTGRES_CONTAINER_NAME" du -sh "$LOG_DIR" 2>/dev/null | cut -f1 || echo "Unknown")

echo "=== Cleanup Summary ==="
echo "Final log file count: $FINAL_COUNT"
echo "Total log directory size: $TOTAL_SIZE"
echo "Files removed by age: $REMOVED_BY_AGE"
echo "Files rotated by size: $ROTATED_COUNT"
echo "Old rotated files removed: $REMOVED_ROTATED"

# Show remaining log files
echo "=== Current Log Files ==="
docker exec "$POSTGRES_CONTAINER_NAME" ls -lah "$LOG_DIR" 2>/dev/null || echo "Unable to list log directory"

echo "Log cleanup completed successfully"