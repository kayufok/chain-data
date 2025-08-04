# Backend Requirements Documentation – Iteration 2

## Feature/Module Name

Connectivity Execution and Verification for Containerized Backend Infrastructure

## Service Overview

- **Service Name:** backend-java-springboot-postgres
- **Technology Stack:** Java 21, Spring Boot 3.5.4, Gradle, PostgreSQL 17, Docker, Docker Compose, Shell scripting
- **Architecture Pattern:** Microservice-inspired, containerized
- **Deployment Target:** Docker
- **Infrastructure Scripts Location:** All scripts reside in `/infrastructure`
- **Docker Network:** All components must use the network named `nginx-net`


## Iteration 2 Objective

- **Goal:** As a backend developer, ensure that the full lifecycle is tested—scripts are executed to start infrastructure, containers are running, and the Java Spring Boot app successfully connects to Postgres database over the `nginx-net` Docker network.


## Functional Requirements

### Execution \& Connectivity Verification

| Requirement | Description |
| :-- | :-- |
| Infrastructure Startup | Execute the provided scripts in `/infrastructure` in the documented order, resulting in running containers on `nginx-net` |
| Application Connectivity | Confirm that the Java Spring Boot application is able to connect to the Postgres 17 database upon startup |
| Verification Checks | Provide and execute a script/command to verify: <br> - Both containers are running and networked <br> - App logs confirm a successful DB connection |
| Cleanup | Ability to clean up containers and volumes after test (optional for debugging repeats) |

## Business Logic Requirements

- Execution order must be clear:

1. Create Docker network (`network-create.sh`)
2. Start Postgres container (`db-start.sh`)
3. Build/Run Java app container (`app-build-run.sh`)
- Verification step must check:
    - Postgres responds on expected port inside network
    - Application logs show a successful DB connection (e.g., “Connected to database” on startup)
    - Exit code and status messages must clearly indicate pass/fail
- Provide a single composite verification script (`verify-connectivity.sh`) in `/infrastructure` (or clear manual steps) that:
    - Confirms container status (`docker ps`)
    - Retrieves and inspects application logs for DB connection success message


## Non-Functional Requirements

- **Testing Scripts:** All test and verification scripts are stored in `/infrastructure` and documented with expected outcome messages
- **Safety:** Clean up (teardown) scripts are available but not destructive by default
- **Logs:** App and DB logs are accessible for inspection after test execution
- **Documentation:** Step-by-step instructions for execution and verification included in project onboarding and `/infrastructure/README.md`


## Acceptance Criteria

- **AC1:** All required Docker containers (app, database) are running and attached to `nginx-net` after execution of startup scripts
- **AC2:** Java Spring Boot application logs clearly show a successful connection to the Postgres database
- **AC3:** Verification script confirms connectivity and prints “SUCCESS: Application connected to database” (or similar)
- **AC4:** Any failures are caught and clearly logged in the output; troubleshooting steps provided if failure occurs


## Definition of Done

- All startup and verification scripts are in `/infrastructure`, executable cross-platform, and have been run/tested successfully
- Documentation provides stepwise run/test/verify instructions
- Product Owner and backend team review/acceptance


## Technical Notes

- Use `docker network inspect nginx-net`, `docker ps`, and application log inspection in the verification script
- If using Docker Compose, include `depends_on` and appropriate health checks for service startup sequencing
- Verification can be automated or manually invoked via a separate script in `/infrastructure`


## Comments and Discussion

- This iteration focuses on test-driven infrastructure and application readiness for all backend developers involved
- Connectivity verification is essential before additional features or environment promotion

---
