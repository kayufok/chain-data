# Backend Requirements Documentation

## Feature/Module Name

Batch Process for Ethereum Address Transaction Data Collection and Storage

## Service Overview

- **Service Name:** ethereum-batch-processor-service
- **Technology Stack:** Java 21, Spring Boot 3.1.4, Gradle, Spring Batch, Spring Scheduler, MyBatis Plus, PostgreSQL 17
- **Architecture Pattern:** Batch Processing Service with Scheduled Jobs
- **Deployment Target:** Docker (on nginx-net network)
- **External Dependencies:** Ankr Ethereum RPC API, existing ethereum-block-address-service


## Functional Requirements

### Batch Processing Components

#### Scheduled Batch Job

- **Job Name:** EthereumAddressBatchProcessor
- **Schedule:** Configurable via cron expression (default: every 5 minutes)
- **Trigger:** Automatic based on schedule or manual trigger via API endpoint


#### Processing Logic Flow

| Step | Description | Implementation |
| :-- | :-- | :-- |
| 1. Initialize | Read next_block_number from chain_info table for Ethereum chain | Query chain_info where chain_id = "1" |
| 2. Create Batch | Generate list of block numbers to process (current to current + batch_size) | Create array of block numbers |
| 3. Rate Limited Processing | Process each block with API rate limiting | Use rate limiter with configured requests per minute |
| 4. Address Extraction | Extract unique addresses from block transactions | Reuse RPC service from US-002 |
| 5. Database Storage | Store addresses and relationships | Bulk insert operations |
| 6. Update Progress | Update next_block_number | Update chain_info table |

### API Endpoints

#### Batch Job Management

| Method | Endpoint | Description | Request Body | Response |
| :-- | :-- | :-- | :-- | :-- |
| POST | `/api/v1/batch/start` | Start batch processing manually | N/A | Job status |
| POST | `/api/v1/batch/stop` | Stop current batch processing | N/A | Stop confirmation |
| GET | `/api/v1/batch/status` | Get current batch job status | N/A | Job status and metrics |
| GET | `/api/v1/batch/metrics` | Get processing metrics | N/A | Performance statistics |

**Batch Status Response:**

```json
{
  "status": "success",
  "data": {
    "jobStatus": "RUNNING",
    "currentBlockNumber": 18500150,
    "blocksProcessed": 50,
    "blocksRemaining": 50,
    "addressesFound": 1250,
    "failedBlocks": 2,
    "estimatedCompletionTime": "2024-01-01T00:05:00Z",
    "processingRate": "10 blocks/minute"
  }
}
```


### Business Logic Requirements

- **Validation Rules:**
    - Block numbers must be positive integers
    - Batch size must be between 1 and 1000
    - Rate limit must be between 1 and 3000 requests per minute
    - Chain ID must exist in chain_info table
- **Business Rules:**
    - Process blocks sequentially from next_block_number
    - Store only unique addresses (avoid duplicates using wallet_address unique constraint)
    - Create address-chain relationships only if combination doesn't exist
    - Update next_block_number after each batch regardless of success/failure
    - Log all API failures with detailed error information
- **Data Processing:**

1. Query chain_info.next_block_number for target chain
2. Create batch array: [next_block_number, next_block_number+1, ..., next_block_number+batch_size-1]
3. For each block in batch:
        - Call RPC API with rate limiting
        - Extract addresses from transaction data
        - Store new addresses in address table
        - Create address-chain relationships
        - Log failures if API call fails
4. Update chain_info.next_block_number = next_block_number + batch_size
- **External Integrations:**
    - **Ethereum RPC Service:** Reuse service from US-002 for block data retrieval
    - **Database Entities:** Use entities from US-003 for data persistence


## Non-Functional Requirements

### Performance Requirements

- **Processing Speed:** Complete 100-block batch within 10 minutes under normal conditions
- **Rate Limiting:** Never exceed configured API requests per minute (default: 1500/min)
- **Concurrent Processing:** Single-threaded processing to maintain rate limiting accuracy
- **Resource Usage:** Maximum 1GB memory usage during batch processing


### Reliability Requirements

- **Availability:** 99.5% uptime for batch processing service
- **Error Handling:**
    - Transient errors: Retry with exponential backoff (max 3 attempts)
    - Permanent errors: Log and continue to next block
    - Critical errors: Stop processing and alert operations team
- **Logging Strategy:**
    - INFO: Batch start/completion, progress updates every 10 blocks
    - WARN: API timeouts, retries, duplicate address attempts
    - ERROR: API failures, database errors, batch termination
- **Monitoring:**
    - Health check endpoint: `/actuator/health`
    - Metrics endpoint: `/actuator/metrics`
    - Custom metrics: blocks processed, addresses stored, error rates


### Scalability Requirements

- **Horizontal Scaling:** Single instance processing to avoid duplicate work
- **Vertical Scaling:** Configurable batch sizes for performance optimization
- **Database Scaling:** Bulk insert operations for improved throughput


### External Dependencies

- **Ethereum RPC Service (US-002):**
    - SLA: Dependent on existing service availability
    - Rate limits: Respect 1500 requests per minute limit
    - Timeout: 10 seconds per request
    - Retry policy: 3 attempts with exponential backoff


## Configuration Management

- **Environment Variables:**
    - `BATCH_SIZE`: Number of blocks per batch (default: 100)
    - `RATE_LIMIT_PER_MINUTE`: API requests per minute (default: 1500)
    - `RETRY_ATTEMPTS`: Max retry attempts (default: 3)
    - `BATCH_SCHEDULE_CRON`: Job schedule (default: "0 */5 * * * *")
    - `CHAIN_ID`: Target chain ID (default: "1")
    - `BATCH_PROCESSING_ENABLED`: Enable/disable batch processing (default: true)
- **Feature Flags:**
    - `BATCH_METRICS_ENABLED`: Enable detailed metrics collection
    - `BATCH_ALERTS_ENABLED`: Enable alerting for failures
    - `BULK_INSERT_ENABLED`: Use bulk insert operations
- **Secrets Management:**
    - Database credentials via environment variables
    - RPC endpoint configuration from existing service


## Testing Requirements

- **Unit Test Coverage:** Minimum 85% code coverage
- **Integration Tests:**
    - End-to-end batch processing with mock RPC responses
    - Database persistence verification
    - Rate limiting compliance testing
    - Error handling scenarios (API failures, database errors)
- **Performance Testing:**
    - Batch processing speed with various batch sizes
    - Memory usage during large batch operations
    - Rate limiting accuracy over time
- **Load Testing:**
    - Continuous processing over extended periods
    - Database performance under high insert volume


## Technical Architecture

### Service Components

1. **Batch Job Scheduler:** Spring Scheduler for automated job execution
2. **Batch Processor Service:** Core business logic for batch processing
3. **Rate Limiter:** Token bucket algorithm for API rate limiting
4. **RPC Client Service:** Integration with existing ethereum-block-address-service
5. **Database Service:** Bulk operations for address and relationship storage
6. **Monitoring Service:** Metrics collection and health monitoring

### Data Flow

1. Scheduler triggers batch job at configured intervals
2. Read next_block_number from chain_info table
3. Create batch of block numbers to process
4. For each block:
    - Apply rate limiting
    - Call RPC service to get block data
    - Extract unique addresses from transactions
    - Store addresses and relationships in database
    - Log any failures
5. Update next_block_number in chain_info
6. Log batch completion metrics

### Database Operations

```java
// Bulk insert addresses
INSERT INTO address (wallet_address) 
VALUES (?) ON CONFLICT (wallet_address) DO NOTHING;

// Bulk insert address-chain relationships
INSERT INTO address_chain (wallet_address_id, chain_id) 
SELECT a.id, ? FROM address a WHERE a.wallet_address = ? 
ON CONFLICT (wallet_address_id, chain_id) DO NOTHING;

// Update next block number
UPDATE chain_info SET next_block_number = ? WHERE chain_id = ?;
```


## Acceptance Criteria Implementation

- **AC1:** Batch processor reads next_block_number from chain_info table for Ethereum chain
- **AC2:** RPC API calls extract addresses and store new ones in address table
- **AC3:** Address-chain relationships created only for new combinations
- **AC4:** API failures logged in api_call_failure_log with complete error details
- **AC5:** Configurable batch size (100 default) and rate limiting (1500/min default)
- **AC6:** next_block_number updated after each batch completion
- **AC7:** Process stops on consecutive failures with proper logging and cleanup


## Definition of Done

- Batch processing service implemented with Spring Batch/Scheduler
- Integration with existing RPC service (US-002) and database entities (US-003)
- Configurable batch size and rate limiting implemented
- Comprehensive error handling and logging
- Unit and integration tests with 85%+ coverage
- Performance testing validates rate limiting compliance
- Monitoring and alerting configured
- Service deployed and tested on nginx-net network
- API documentation updated
- Business Analyst acceptance testing completed


## Comments and Discussion

- Consider implementing dead letter queue for consistently failing blocks
- Database connection pooling optimized for bulk operations
- Monitoring dashboard for real-time batch processing visibility
- Circuit breaker pattern for RPC API resilience
- Future enhancement: parallel processing for multiple blockchain networks

