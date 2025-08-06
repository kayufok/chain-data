# Backend Requirements Documentation

## Feature/Module Name

Database Entity Setup and MyBatis Plus Integration for RPC Data Storage

## Service Overview

- **Service Name:** blockchain-data-entity-service
- **Technology Stack:** Java 21, Spring Boot 3.5.4, Gradle, MyBatis Plus, PostgreSQL 17, Jackson (JSON processing)
- **Architecture Pattern:** Microservice, RESTful API with ORM
- **Deployment Target:** Docker (on nginx-net network)
- **Database:** PostgreSQL 17 (existing infrastructure)


## Functional Requirements

### Database Schema Implementation

#### Table Structures

**Address Table:**

```sql
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    wallet_address TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Chain Info Table:**

```sql
CREATE TABLE chain_info (
    id BIGSERIAL PRIMARY KEY,
    chain_name TEXT NOT NULL,
    chain_id TEXT NOT NULL UNIQUE,
    next_block_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Address Chain Junction Table:**

```sql
CREATE TABLE address_chain (
    id BIGSERIAL PRIMARY KEY,
    wallet_address_id BIGINT NOT NULL REFERENCES address(id) ON DELETE CASCADE,
    chain_id BIGINT NOT NULL REFERENCES chain_info(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(wallet_address_id, chain_id)
);
```

**Status Reference Table:**

```sql
CREATE TABLE status (
    id BIGSERIAL PRIMARY KEY,
    status_type TEXT NOT NULL,
    status_code TEXT NOT NULL UNIQUE,
    status_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**API Call Failure Log Table:**

```sql
CREATE TABLE api_call_failure_log (
    id BIGSERIAL PRIMARY KEY,
    chain_id TEXT NOT NULL REFERENCES chain_info(chain_id),
    block_number BIGINT NOT NULL,
    status_code TEXT NOT NULL REFERENCES status(status_code),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```


### API Endpoints

#### Address Entity Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| GET | `/api/v1/addresses` | Get all addresses | N/A | Address list |
| GET | `/api/v1/addresses/{id}` | Get address by ID | N/A | Single address |
| POST | `/api/v1/addresses` | Create new address | Address object | Created address |
| PUT | `/api/v1/addresses/{id}` | Update address | Address object | Updated address |
| DELETE | `/api/v1/addresses/{id}` | Delete address | N/A | Success/Error |

**Address Entity Request/Response:**

```json
{
  "id": 1,
  "walletAddress": "0x1234567890abcdef1234567890abcdef12345678",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```


#### Chain Info Entity Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| GET | `/api/v1/chains` | Get all chains | N/A | Chain list |
| GET | `/api/v1/chains/{id}` | Get chain by ID | N/A | Single chain |
| POST | `/api/v1/chains` | Create new chain | Chain object | Created chain |
| PUT | `/api/v1/chains/{id}` | Update chain | Chain object | Updated chain |
| DELETE | `/api/v1/chains/{id}` | Delete chain | N/A | Success/Error |

**Chain Info Entity Request/Response:**

```json
{
  "id": 1,
  "chainName": "Ethereum",
  "chainId": "1",
  "nextBlockNumber": 18500000,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```


#### Address-Chain Relationship Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| GET | `/api/v1/address-chains` | Get all relationships | N/A | Relationship list |
| GET | `/api/v1/address-chains/{id}` | Get relationship by ID | N/A | Single relationship |
| POST | `/api/v1/address-chains` | Create relationship | Relationship object | Created relationship |
| DELETE | `/api/v1/address-chains/{id}` | Delete relationship | N/A | Success/Error |

#### Status Entity Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| GET | `/api/v1/statuses` | Get all statuses | N/A | Status list |
| GET | `/api/v1/statuses/{id}` | Get status by ID | N/A | Single status |
| POST | `/api/v1/statuses` | Create new status | Status object | Created status |
| PUT | `/api/v1/statuses/{id}` | Update status | Status object | Updated status |
| DELETE | `/api/v1/statuses/{id}` | Delete status | N/A | Success/Error |

#### API Call Failure Log Endpoints

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| GET | `/api/v1/failure-logs` | Get all failure logs | N/A | Failure log list |
| GET | `/api/v1/failure-logs/{id}` | Get failure log by ID | N/A | Single failure log |
| POST | `/api/v1/failure-logs` | Create failure log | Failure log object | Created log |
| DELETE | `/api/v1/failure-logs/{id}` | Delete failure log | N/A | Success/Error |

### Business Logic Requirements

- **Validation Rules:**
    - Wallet addresses must follow Ethereum address format (42 characters, starting with 0x)
    - Chain ID must be unique across all chains
    - Address-chain relationships must be unique (no duplicates)
    - Status codes must be unique
    - Block numbers must be positive integers
- **Business Rules:**
    - Cascade delete: When an address is deleted, all related address-chain relationships are removed
    - Cascade delete: When a chain is deleted, all related address-chain relationships are removed
    - Foreign key integrity must be maintained across all relationships
    - Audit trail: All entities include created_at and updated_at timestamps
- **Data Processing:**
    - Auto-generate primary keys for all entities
    - Automatic timestamp updates on record modifications
    - Case-sensitive wallet address storage (preserve checksummed format)
    - Efficient querying through proper indexing


## Non-Functional Requirements

### Performance Requirements

- **Response Time:** Maximum 1 second for single entity operations
- **Response Time:** Maximum 3 seconds for complex queries with joins
- **Concurrent Users:** Support up to 100 concurrent database connections
- **Resource Usage:** Maximum 512MB additional memory for ORM operations


### Security Requirements

- **Input Validation:** Strict validation for all entity fields
- **SQL Injection Prevention:** MyBatis Plus parameterized queries
- **Data Integrity:** Foreign key constraints and unique constraints enforced
- **Audit Trail:** All operations logged with timestamps


### Reliability Requirements

- **Availability:** 99.5% uptime for database operations
- **Error Handling:** Graceful handling of constraint violations and database errors
- **Transaction Management:** Atomic operations for multi-table updates
- **Logging Strategy:**
    - INFO: Successful CRUD operations
    - WARN: Constraint violations, duplicate key attempts
    - ERROR: Database connection failures, transaction rollbacks


### Scalability Requirements

- **Database Indexing:** Proper indexes on frequently queried fields
- **Connection Pooling:** Efficient database connection management
- **Query Optimization:** Optimized queries for large datasets


## Configuration Management

### Database Configuration

- **Environment Variables:**
    - `DATABASE_URL`: PostgreSQL connection string
    - `DATABASE_USERNAME`: Database username
    - `DATABASE_PASSWORD`: Database password
    - `DATABASE_POOL_SIZE`: Connection pool size (default: 10)


### MyBatis Plus Configuration

- **MyBatis Plus Properties:**
    - `mybatis-plus.global-config.db-config.id-type`: AUTO
    - `mybatis-plus.global-config.db-config.table-underline`: true
    - `mybatis-plus.configuration.map-underscore-to-camel-case`: true
    - `mybatis-plus.configuration.log-impl`: org.apache.ibatis.logging.stdout.StdOutImpl


## Testing Requirements

- **Unit Test Coverage:** Minimum 85% code coverage
- **Integration Tests:**
    - Database connectivity testing
    - CRUD operation testing for all entities
    - Foreign key constraint testing
    - Unique constraint testing
- **Performance Testing:**
    - Query performance with large datasets
    - Connection pool stress testing


## Technical Architecture

### Entity Classes Structure

```java
@Data
@TableName("address")
public class Address {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("wallet_address")
    private String walletAddress;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```


### Service Layer Components

1. **Entity Services:** Business logic for each entity type
2. **Mapper Interfaces:** MyBatis Plus data access layer
3. **Controller Layer:** REST API endpoints
4. **DTO Classes:** Data transfer objects for API responses
5. **Validation Layer:** Input validation and constraint checking

### Database Migration Strategy

- **Migration Scripts:** Versioned SQL scripts in `/infrastructure/database/migrations/`
- **Migration Tool:** Flyway or Liquibase for database versioning
- **Rollback Support:** Down migration scripts for each version


## Acceptance Criteria Implementation

- **AC1:** Database schema created with all five tables and proper constraints
- **AC2:** MyBatis Plus entities implemented with correct annotations and mappings
- **AC3:** RESTful CRUD endpoints implemented for all entities
- **AC4:** Many-to-many relationship handling via address_chain junction table
- **AC5:** Foreign key relationships properly configured and queried
- **AC6:** All basic CRUD operations tested and working correctly


## Definition of Done

- Database migration scripts created and tested
- All five entity classes implemented with MyBatis Plus
- MyBatis Plus configuration completed
- RESTful API endpoints implemented and documented
- Unit and integration tests with 85%+ coverage
- API documentation updated (Swagger/OpenAPI)
- Database deployed to development environment
- Code reviewed and approved
- Business Analyst acceptance testing completed


## Comments and Discussion

- Consider adding database indexes on frequently queried fields (wallet_address, chain_id, block_number)
- Future stories will implement data population from RPC API responses
- Database audit fields (created_at, updated_at) added for better tracking
- Foreign key reference in api_call_failure_log adjusted to use status_code for consistency

