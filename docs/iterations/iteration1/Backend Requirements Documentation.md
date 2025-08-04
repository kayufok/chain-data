# Backend Requirements Documentation

## Feature/Module Name

Containerized Backend Infrastructure – Java Spring Boot Service with Postgres

## Service Overview

- **Service Name:** backend-java-springboot-postgres
- **Technology Stack:** Java 21, Spring Boot 3.5.4, Gradle, PostgreSQL 17, Docker, Docker Compose, Shell scripting
- **Architecture Pattern:** Microservice-inspired, containerized
- **Deployment Target:** Docker
- **Infrastructure Scripts Location:** All scripts are stored in the `/infrastructure` directory at the project root.
- **Docker Network Name:** `nginx-net` (all containers/services must use this network)


## Functional Requirements

### Infrastructure Scripts (All Stored in /infrastructure)

| Requirement | Description |
| :-- | :-- |
| Docker Network Creation | Script (`network-create.sh`) must create a Docker network named `nginx-net` if it doesn’t exist |
| Postgres 17 Container | Docker script (`db-start.sh`) creates a persistent Postgres 17 container on the `nginx-net` network |
| Database Backup and Restore | Scripts (`db-backup.sh`, `db-restore.sh`) handle Postgres backup to storage and restore using files in `/infrastructure` |
| Database Logs Cleanup | Script (`db-log-cleanup.sh`) deletes/moves Postgres logs as per retention policy, configurable with environment variables |
| Spring Boot Application Container | Docker script (`app-build-run.sh`) builds/runs the Java app (Java 21, Spring Boot 3.5.4, Gradle), connected to `nginx-net` |

### Requirements for /infrastructure Scripts

- **Folder Convention:** All infrastructure scripts and config/examples must reside in `/infrastructure`
- **Naming:** Scripts must use clear, descriptive names (as above)
- **Documentation:** Each script contains header comments with purpose, usage, required env variables, and example commands
- **Executability:** Scripts must be executable or instructions provide `chmod +x`
- **Environment Variables:** An example `.env` file is provided in `/infrastructure` (never checked into VCS)
- **Safety:** All scripts must check for prerequisites (e.g., Docker present, `nginx-net` already created)
- **Idempotency:** Scripts can be safely rerun without causing duplicate or destructive actions


## Business Logic Requirements

- Scripts in `/infrastructure` enforce the use of `nginx-net` Docker network for all backend and database containers
- Backup procedures prevent unintentional file overwrites
- Restore procedures require user confirmation before destructive actions
- Log cleanup policy must be configurable by environment variable or command argument
- Docker volumes are used for database data; no data is stored in ephemeral containers


## Non-Functional Requirements

### Performance

- All infrastructure scripts should complete operations in ≤2min under normal loads


### Security

- No secrets in `/infrastructure` scripts; use `.env` and Docker secrets as needed
- All container ports are only exposed as required by application logic and not published externally without reason
- Scripts validate environment before performing potentially destructive actions


### Reliability

- Containers using `nginx-net` auto-restart on failure
- Data volume persistence is enforced for database and log containers
- All script logs go to stdout, with optional log file support under `/infrastructure/logs`


### Scalability

- Application containers must be able to scale horizontally via Docker Compose and use `nginx-net`
- Database scaling (replication, clustering) is out of initial scope


## Configuration Management

- All required environment variables documented in the `.env.example` file in `/infrastructure`
- Each script must check for required configuration before execution and provide actionable error messages


## Testing Requirements

- Each infrastructure script must include or reference testing commands to verify:
    - The correct creation/connection to `nginx-net`
    - Database container state and availability
    - Backup, restore, and cleanup success/failure cases


## Acceptance Criteria

- **AC0:** `/infrastructure/network-create.sh` creates the `nginx-net` Docker network (idempotent)
- **AC1:** `/infrastructure/db-start.sh` launches Postgres 17 on that network, with persistent volume
- **AC2:** `/infrastructure/db-backup.sh` and `/infrastructure/db-restore.sh` handle Postgres dump/import with files in `/infrastructure`
- **AC3:** `/infrastructure/db-log-cleanup.sh` cleans up logs with retention management controlled via `.env`
- **AC4:** `/infrastructure/app-build-run.sh` builds and runs the Java (Java 21, Spring Boot 3.5.4, Gradle) container and ensures it is attached to `nginx-net`


## Definition of Done

- All scripts reside in `/infrastructure`, are cross-platform tested, and covered with README/examples
- Usage, configuration, and output of each script documented in script headers and main onboarding guide
- Product Owner review and acceptance


## Technical Notes

- Use Docker Compose where appropriate, but require all scripts, configs, and auxiliary files to reside in `/infrastructure`
- Use consistent and well-documented file/volume paths, ideally relative to `/infrastructure` or configurable via `.env`
- Enforce use of `nginx-net` via explicit network arguments or Compose `networks:` specification


## Comments and Discussion

- Centralizing infrastructure in `/infrastructure` boosts discoverability and auditability
- The `nginx-net` network will facilitate easy integration of future reverse proxy or additional service containers

