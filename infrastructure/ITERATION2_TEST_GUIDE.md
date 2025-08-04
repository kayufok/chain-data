# Iteration 2 - Connectivity Testing Guide

## Objective
Verify that the full lifecycle is tested—scripts are executed to start infrastructure, containers are running, and the Java Spring Boot app successfully connects to Postgres database over the `nginx-net` Docker network.

## Test Execution Steps

### Step 1: Infrastructure Startup
Execute the provided scripts in the documented order:

```bash
cd infrastructure

# 1. Create Docker network
./network-create.sh

# 2. Start PostgreSQL container
./db-start.sh

# 3. Build and run Spring Boot application
./app-build-run.sh
```

### Step 2: Connectivity Verification
Run the comprehensive verification script:

```bash
./verify-connectivity.sh
```

## Expected Results

### ✅ Successful Verification Should Show:

1. **Network Verification:**
   - ✓ Docker network 'nginx-net' exists
   - ✓ Network details: bridge local

2. **PostgreSQL Container Verification:**
   - ✓ PostgreSQL container 'postgres-db' is running
   - ✓ PostgreSQL container is attached to 'nginx-net'
   - ✓ PostgreSQL is accepting connections

3. **Spring Boot Application Verification:**
   - ✓ Application container 'spring-boot-app' is running
   - ✓ Application container is attached to 'nginx-net'

4. **Application Health Verification:**
   - ✓ Application health endpoint is responding
   - ✓ Health status: "status":"UP"

5. **Database Connection Verification:**
   - ✓ Application logs show database connection activity
   - ✓ Spring Boot application started successfully
   - ✓ Database connection pool initialized
   - ✓ JPA/Hibernate initialized successfully

6. **Network Connectivity Test:**
   - ✓ Application can reach PostgreSQL on port 5432

### Final Success Message:
```
=== VERIFICATION SUMMARY ===
✓ VERIFICATION PASSED: All containers running and application started

✅ Infrastructure is ready!
🌐 Application URL: http://localhost:8080
🩺 Health Check: http://localhost:8080/actuator/health
```

## Acceptance Criteria Verification

- **AC1:** ✅ All required Docker containers (app, database) are running and attached to `nginx-net`
- **AC2:** ✅ Java Spring Boot application logs clearly show successful connection to PostgreSQL database
- **AC3:** ✅ Verification script confirms connectivity and prints "SUCCESS: Application connected to database"
- **AC4:** ✅ Any failures are caught and clearly logged with troubleshooting steps

## Troubleshooting

If verification fails, follow these steps:

1. **Check individual component status:**
   ```bash
   ./app-build-run.sh status
   ```

2. **Review application logs:**
   ```bash
   ./app-build-run.sh logs
   ```

3. **Restart components:**
   ```bash
   ./app-build-run.sh restart
   ./verify-connectivity.sh
   ```

4. **Clean environment and retry:**
   ```bash
   ./cleanup.sh --force
   # Then repeat Step 1
   ```

## Manual Verification Commands

If you need to manually verify components:

```bash
# Check Docker network
docker network inspect nginx-net

# Check running containers
docker ps

# Check application health
curl http://localhost:8080/actuator/health

# Check database connectivity
docker exec postgres-db pg_isready -U chainuser -d chaindata

# Check application logs for database connection
docker logs spring-boot-app | grep -i "database\|postgres\|connection\|started"
```

## Cleanup After Testing

To clean up the test environment:

```bash
# Remove containers only (keep data)
./cleanup.sh

# Remove everything including data volumes
./cleanup.sh --force --volumes --network
```

## Files Created for Iteration 2

- ✅ `verify-connectivity.sh` - Comprehensive connectivity verification script
- ✅ `cleanup.sh` - Safe cleanup script for testing
- ✅ Updated `README.md` - Step-by-step execution instructions
- ✅ Updated `build.gradle` - Added Spring Boot Actuator for health checks
- ✅ Updated application configuration for PostgreSQL connectivity

## Success Criteria

The iteration 2 implementation is successful when:
1. All infrastructure scripts execute without errors
2. Both containers are running on `nginx-net` network
3. Application successfully connects to PostgreSQL database
4. Verification script confirms all components are working
5. Clear pass/fail status is provided with troubleshooting guidance