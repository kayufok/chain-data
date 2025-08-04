#!/bin/bash

# Infrastructure Script: PostgreSQL Database Backup
# Purpose: Creates a backup of the PostgreSQL database
# Usage: ./db-backup.sh [backup_name]
# Required Environment Variables: See .env.example
# Example: ./db-backup.sh my-backup-20240101

set -e

# Load environment variables if .env file exists
if [ -f "$(dirname "$0")/.env" ]; then
    source "$(dirname "$0")/.env"
fi

# Default values
POSTGRES_CONTAINER_NAME=${POSTGRES_CONTAINER_NAME:-postgres-db}
POSTGRES_DB=${POSTGRES_DB:-chaindata}
POSTGRES_USER=${POSTGRES_USER:-chainuser}
BACKUP_DIR=${BACKUP_DIR:-./backups}
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-30}

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Generate backup filename
if [ -n "$1" ]; then
    BACKUP_NAME="$1"
else
    BACKUP_NAME="backup-$(date +%Y%m%d-%H%M%S)"
fi

BACKUP_FILE="$BACKUP_DIR/${BACKUP_NAME}.sql"

echo "=== PostgreSQL Database Backup Script ==="
echo "Container: $POSTGRES_CONTAINER_NAME"
echo "Database: $POSTGRES_DB"
echo "Backup File: $BACKUP_FILE"

# Check prerequisites
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed or not in PATH"
    exit 1
fi

# Check if container is running
if ! docker ps | grep -q "$POSTGRES_CONTAINER_NAME"; then
    echo "ERROR: PostgreSQL container '$POSTGRES_CONTAINER_NAME' is not running"
    echo "Please run ./db-start.sh first"
    exit 1
fi

# Check if backup file already exists
if [ -f "$BACKUP_FILE" ]; then
    echo "WARNING: Backup file '$BACKUP_FILE' already exists"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Backup cancelled"
        exit 1
    fi
fi

# Create database backup
echo "INFO: Creating database backup..."
docker exec "$POSTGRES_CONTAINER_NAME" pg_dump \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" \
    --no-password \
    --verbose \
    --clean \
    --if-exists \
    --create > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "SUCCESS: Database backup created: $BACKUP_FILE"
    
    # Show backup file info
    echo "Backup file size: $(du -h "$BACKUP_FILE" | cut -f1)"
    echo "Backup file path: $(realpath "$BACKUP_FILE")"
else
    echo "ERROR: Database backup failed"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Clean up old backups based on retention policy
echo "INFO: Cleaning up old backups (retention: $BACKUP_RETENTION_DAYS days)"
find "$BACKUP_DIR" -name "backup-*.sql" -type f -mtime +$BACKUP_RETENTION_DAYS -delete

# List recent backups
echo "=== Recent Backups ==="
ls -lth "$BACKUP_DIR"/*.sql 2>/dev/null | head -5 || echo "No backup files found"