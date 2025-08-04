# Infrastructure Scripts

This directory contains all the infrastructure scripts and configuration files for the containerized backend system.

## Overview

The system consists of:
- **PostgreSQL 17** database container
- **Spring Boot** application container (Java 21)
- **Docker network** (`nginx-net`) for container communication
- **Backup and restore** functionality
- **Log management** and cleanup

## Prerequisites

- Docker and Docker Compose installed
- Java 21 (for local development)
- Gradle (included via wrapper)
- curl (for health checks)

## Quick Start

### Full Infrastructure Setup and Verification

Follow these steps in order to set up and verify the complete containerized backend:

1. **Create the Docker network:**
   ```bash
   ./network-create.sh
   ```

2. **Start the PostgreSQL database:**
   ```bash
   ./db-start.sh
   ```

3. **Build and run the Spring Boot application:**
   ```bash
   ./app-build-run.sh
   ```

4. **Verify connectivity and database connection:**
   ```bash
   ./verify-connectivity.sh
   ```

The application will be available at: http://localhost:8080

### Quick Status Check

```bash
./app-build-run.sh status
```

## Environment Configuration

Copy the example environment file and customize it:
```bash
cp .env.example .env
# Edit .env with your preferred settings
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DOCKER_NETWORK_NAME` | `nginx-net` | Docker network name |
| `POSTGRES_CONTAINER_NAME` | `postgres-db` | PostgreSQL container name |
| `POSTGRES_DB` | `chaindata` | Database name |
| `POSTGRES_USER` | `chainuser` | Database username |
| `POSTGRES_PASSWORD` | `changeme123` | Database password |
| `POSTGRES_PORT` | `5432` | Database port |
| `APP_CONTAINER_NAME` | `spring-boot-app` | Application container name |
| `APP_PORT` | `8080` | Application port |
| `BACKUP_RETENTION_DAYS` | `30` | Backup retention period |
| `LOG_RETENTION_DAYS` | `7` | Log retention period |

## Scripts Documentation

### network-create.sh
Creates the Docker network for container communication.
```bash
./network-create.sh
```
- **Purpose:** Creates `nginx-net` Docker network
- **Idempotent:** Safe to run multiple times
- **Prerequisites:** Docker daemon running

### db-start.sh
Starts the PostgreSQL 17 database container.
```bash
./db-start.sh
```
- **Purpose:** Creates and starts PostgreSQL container with persistent storage
- **Prerequisites:** Docker network must exist (`./network-create.sh`)
- **Features:** Auto-restart on failure, persistent volumes, health checks

### db-backup.sh
Creates a backup of the PostgreSQL database.
```bash
./db-backup.sh [backup_name]
```
- **Purpose:** Creates SQL dump backup
- **Example:** `./db-backup.sh production-backup-20240101`
- **Location:** Backups stored in `./backups/` directory
- **Features:** Automatic cleanup of old backups, overwrite protection

### db-restore.sh
Restores database from a backup file.
```bash
./db-restore.sh <backup_file>
```
- **Purpose:** Restores database from SQL dump
- **Example:** `./db-restore.sh ./backups/backup-20240101-120000.sql`
- **Safety:** Creates safety backup before restore, requires confirmation
- **Prerequisites:** Database container running (`./db-start.sh`)

### db-log-cleanup.sh
Manages PostgreSQL log files according to retention policy.
```bash
./db-log-cleanup.sh [retention_days] [max_size]
```
- **Purpose:** Cleans up old logs and rotates large files
- **Example:** `./db-log-cleanup.sh 7 100M`
- **Features:** Age-based cleanup, size-based rotation, configurable retention

### app-build-run.sh
Manages the Spring Boot application container.
```bash
./app-build-run.sh [action]
```

**Actions:**
- `build` - Build application and Docker image
- `run` - Build and start application (default)
- `stop` - Stop application container
- `restart` - Stop and start application
- `logs` - Show application logs
- `status` - Show application status

**Examples:**
```bash
./app-build-run.sh build     # Build only
./app-build-run.sh run       # Build and run
./app-build-run.sh logs      # View logs
./app-build-run.sh restart   # Restart application
```

### verify-connectivity.sh
Comprehensive verification of infrastructure connectivity and database connection.
```bash
./verify-connectivity.sh
```
- **Purpose:** Verifies all containers are running and connected properly
- **Features:** Network verification, database connectivity, application health checks
- **Output:** Color-coded status messages with clear pass/fail indicators
- **Prerequisites:** All infrastructure components must be started

### cleanup.sh
Safe cleanup of containers, volumes, and networks for testing/debugging.
```bash
./cleanup.sh [--force] [--volumes] [--network]
```
- **Purpose:** Stops and removes infrastructure components
- **Options:** 
  - `--force` - Skip confirmation prompts
  - `--volumes` - Remove data volumes (WARNING: Data loss!)
  - `--network` - Remove Docker network
- **Safety:** Non-destructive by default, requires confirmation for data removal

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │   PostgreSQL    │
│   Application   │◄──►│   Database      │
│   (port 8080)   │    │   (port 5432)   │
└─────────────────┘    └─────────────────┘
         │                        │
         └────────┬─────────────────┘
                  │
         ┌─────────▼──────────┐
         │   nginx-net        │
         │   Docker Network   │
         └────────────────────┘
```

## Data Persistence

- **Database data:** Stored in `postgres-data` Docker volume
- **Database logs:** Stored in `postgres-logs` Docker volume
- **Application logs:** Available via `docker logs`
- **Backups:** Stored in `./backups/` directory

## Health Checks

The system includes comprehensive health monitoring:

1. **Database Health:**
   ```bash
   docker exec postgres-db pg_isready -U chainuser -d chaindata
   ```

2. **Application Health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

3. **Container Status:**
   ```bash
   ./app-build-run.sh status
   ```

## Troubleshooting

### Common Issues

1. **Network doesn't exist:**
   ```bash
   ./network-create.sh
   ```

2. **Database connection failed:**
   ```bash
   ./db-start.sh
   # Wait for PostgreSQL to be ready
   ```

3. **Application won't start:**
   ```bash
   ./app-build-run.sh logs
   # Check logs for errors
   ```

4. **Port already in use:**
   ```bash
   # Change APP_PORT in .env file
   # Or stop conflicting services
   ```

### Log Locations

- **Application logs:** `docker logs spring-boot-app`
- **Database logs:** `docker logs postgres-db`
- **Infrastructure logs:** `./logs/` (if configured)

### Manual Container Management

If needed, you can manage containers manually:

```bash
# Stop all containers
docker stop spring-boot-app postgres-db

# Remove containers
docker rm spring-boot-app postgres-db

# Remove volumes (WARNING: This deletes all data!)
docker volume rm postgres-data postgres-logs

# Remove network
docker network rm nginx-net
```

## Security Notes

- Change default passwords in `.env` file
- Never commit `.env` file to version control
- Database ports are only exposed as needed
- Application runs as non-root user in container
- Use Docker secrets for production deployments

## Step-by-Step Verification Process

### Complete Infrastructure Testing (Iteration 2)

Follow this exact sequence to verify full lifecycle connectivity:

1. **Initialize Infrastructure:**
   ```bash
   cd infrastructure
   ./network-create.sh    # Creates nginx-net Docker network
   ./db-start.sh          # Starts PostgreSQL 17 container
   ./app-build-run.sh     # Builds and starts Spring Boot application
   ```

2. **Run Connectivity Verification:**
   ```bash
   ./verify-connectivity.sh  # Comprehensive connectivity test
   ```

3. **Expected Verification Results:**
   - ✅ Docker network `nginx-net` exists and is active
   - ✅ PostgreSQL container is running and accepting connections
   - ✅ Spring Boot application container is running on `nginx-net`
   - ✅ Application responds to health checks on port 8080
   - ✅ Application logs show successful database connection
   - ✅ Network connectivity between containers is confirmed

4. **Troubleshooting Failed Verification:**
   ```bash
   ./app-build-run.sh logs    # Check application logs
   ./app-build-run.sh status  # Check container status
   ./cleanup.sh               # Clean up for retry (optional)
   ```

### Expected Success Output

When verification passes, you should see:
```
=== VERIFICATION SUMMARY ===
✓ VERIFICATION PASSED: All containers running and application started

✅ Infrastructure is ready!
🌐 Application URL: http://localhost:8080
🩺 Health Check: http://localhost:8080/actuator/health
```

## Development Workflow

1. **Start the infrastructure:**
   ```bash
   ./network-create.sh
   ./db-start.sh
   ```

2. **Develop and test locally:**
   ```bash
   # Make code changes
   ./app-build-run.sh restart
   ./verify-connectivity.sh  # Verify changes work
   ```

3. **Create backups before major changes:**
   ```bash
   ./db-backup.sh before-feature-xyz
   ```

4. **Clean up logs periodically:**
   ```bash
   ./db-log-cleanup.sh
   ```

## Production Considerations

- Set up proper backup scheduling
- Configure log rotation and monitoring
- Use production-grade passwords
- Consider using Docker Compose for multi-environment deployments
- Implement proper secrets management
- Set up monitoring and alerting

## Support

For issues with the infrastructure scripts, check:
1. Script output and error messages
2. Docker daemon status
3. Container logs
4. Network connectivity
5. Environment variable configuration