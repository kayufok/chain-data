#!/bin/bash

# Infrastructure Script: PostgreSQL Database Restore
# Purpose: Restores a PostgreSQL database from a backup file
# Usage: ./db-restore.sh <backup_file>
# Required Environment Variables: See .env.example
# Example: ./db-restore.sh ./backups/backup-20240101-120000.sql

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

echo "=== PostgreSQL Database Restore Script ==="

# Check if backup file is provided
if [ -z "$1" ]; then
    echo "ERROR: Backup file not specified"
    echo "Usage: $0 <backup_file>"
    echo "Available backups:"
    ls -lth "$BACKUP_DIR"/*.sql 2>/dev/null | head -10 || echo "No backup files found in $BACKUP_DIR"
    exit 1
fi

BACKUP_FILE="$1"

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file '$BACKUP_FILE' does not exist"
    exit 1
fi

echo "Container: $POSTGRES_CONTAINER_NAME"
echo "Database: $POSTGRES_DB"
echo "Backup File: $BACKUP_FILE"
echo "Backup File Size: $(du -h "$BACKUP_FILE" | cut -f1)"

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

# Confirm destructive action
echo ""
echo "WARNING: This operation will DROP and RECREATE the database '$POSTGRES_DB'"
echo "All existing data will be PERMANENTLY LOST!"
echo ""
read -p "Are you sure you want to continue? Type 'YES' to confirm: " -r
echo

if [ "$REPLY" != "YES" ]; then
    echo "Restore operation cancelled"
    exit 1
fi

# Verify the backup file is readable
echo "INFO: Validating backup file..."
if ! head -20 "$BACKUP_FILE" | grep -q "PostgreSQL database dump"; then
    echo "WARNING: Backup file does not appear to be a valid PostgreSQL dump"
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Restore cancelled"
        exit 1
    fi
fi

# Create a safety backup before restore
SAFETY_BACKUP="$BACKUP_DIR/pre-restore-backup-$(date +%Y%m%d-%H%M%S).sql"
echo "INFO: Creating safety backup before restore: $SAFETY_BACKUP"
docker exec "$POSTGRES_CONTAINER_NAME" pg_dump \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" \
    --no-password \
    --clean \
    --if-exists \
    --create > "$SAFETY_BACKUP"

# Restore database
echo "INFO: Restoring database from backup..."
cat "$BACKUP_FILE" | docker exec -i "$POSTGRES_CONTAINER_NAME" psql \
    -U "$POSTGRES_USER" \
    -d postgres \
    --no-password

if [ $? -eq 0 ]; then
    echo "SUCCESS: Database restored from backup: $BACKUP_FILE"
    echo "Safety backup created: $SAFETY_BACKUP"
    
    # Verify database connection
    echo "INFO: Verifying database connection..."
    if docker exec "$POSTGRES_CONTAINER_NAME" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1;" &> /dev/null; then
        echo "SUCCESS: Database connection verified"
    else
        echo "WARNING: Database connection verification failed"
    fi
else
    echo "ERROR: Database restore failed"
    echo "Safety backup available at: $SAFETY_BACKUP"
    exit 1
fi

echo "=== Restore Complete ==="