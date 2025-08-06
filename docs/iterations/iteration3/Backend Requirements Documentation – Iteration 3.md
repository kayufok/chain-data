# Backend Requirements Documentation

## Feature/Module Name

RPC API Integration Service for Ethereum Block Transaction Address Retrieval

## Service Overview

- **Service Name:** ethereum-block-address-service
- **Technology Stack:** Java 21, Spring Boot 3.5.4, Gradle, Jackson (JSON processing)
- **Architecture Pattern:** Microservice, RESTful API
- **Deployment Target:** Docker (on nginx-net network)
- **External Dependencies:** Ankr Ethereum RPC API


## Functional Requirements

### API Endpoints

#### Get Block Addresses

- **Method:** GET
- **URL:** `/api/v1/blocks/{blockHeight}/addresses`
- **Description:** Retrieves all unique addresses that have transactions in the specified Ethereum block

**Request Parameters:**

- `blockHeight` (path parameter): Block number in decimal or hexadecimal format (e.g., "18500000" or "0x11a9760")

**Request Example:**

```http
GET /api/v1/blocks/18500000/addresses
Accept: application/json
```

**Success Response (200 OK):**

```json
{
  "status": "success",
  "data": {
    "blockHeight": "18500000",
    "blockHash": "0x...",
    "addresses": [
      "0x1234567890abcdef1234567890abcdef12345678",
      "0xabcdef1234567890abcdef1234567890abcdef12"
    ],
    "transactionCount": 150,
    "uniqueAddressCount": 75,
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

**Error Responses:**

- **400 Bad Request:** Invalid block height format

```json
{
  "status": "error",
  "error": {
    "code": "INVALID_BLOCK_HEIGHT",
    "message": "Block height must be a valid decimal or hexadecimal number"
  }
}
```

- **404 Not Found:** Block not found

```json
{
  "status": "error", 
  "error": {
    "code": "BLOCK_NOT_FOUND",
    "message": "Block with height 18500000 not found"
  }
}
```

- **502 Bad Gateway:** RPC API error

```json
{
  "status": "error",
  "error": {
    "code": "RPC_API_ERROR", 
    "message": "Failed to retrieve block data from RPC provider"
  }
}
```

- **503 Service Unavailable:** RPC timeout

```json
{
  "status": "error",
  "error": {
    "code": "RPC_TIMEOUT",
    "message": "RPC request timed out"
  }
}
```


### Business Logic Requirements

- **Validation Rules:**
    - Block height must be a valid positive integer or hexadecimal string
    - Block height cannot exceed the latest block number
    - Input sanitization to prevent injection attacks
- **Business Rules:**
    - Extract unique addresses from both `from` and `to` fields of all transactions
    - Handle blocks with zero transactions (return empty address list)
    - Preserve original case of Ethereum addresses (checksummed format)
- **Data Processing:**

1. Convert block height to hexadecimal format for RPC call
2. Call Ankr RPC API with `eth_getBlockByNumber` method
3. Parse response and extract transaction data
4. Collect all `from` and `to` addresses from transactions
5. Remove duplicates and null addresses
6. Return structured response with metadata
- **External Integrations:**
    - **Ankr RPC API:** `https://rpc.ankr.com/eth/ee19a9a6fe722d2ce427185a1f75db2a4d414461037af02cde80e6012c518799`
    - **Method:** `eth_getBlockByNumber`
    - **Request Format:** JSON-RPC 2.0


## Non-Functional Requirements

### Performance Requirements

- **Response Time:** Maximum 5 seconds for standard blocks (<500 transactions)
- **Concurrent Users:** N/A


### Reliability Requirements

- **Availability:** 99.5% uptime SLA
- **Error Handling:**
    - Graceful degradation when RPC API is unavailable
    - Exponential backoff for failed RPC requests
    - Circuit breaker pattern for RPC calls
- **Logging Strategy:**
    - INFO: Successful requests with response time
    - WARN: RPC timeouts and retries
    - ERROR: RPC failures and system errors
- **Monitoring:**
    - Health check endpoint: `/actuator/health`
    - Metrics endpoint: `/actuator/metrics`
    - RPC response time tracking


### Scalability Requirements

- **Horizontal Scaling:** Stateless service design for easy horizontal scaling
- **Vertical Scaling:** Efficient memory usage for large blocks
- **Database Scaling:** N/A (stateless service)


### External Dependencies

- **Ankr RPC API:**
    - SLA: 99.9% availability
    - Rate limits: Respect provider's rate limiting (30 req/sec)
    - Timeout: 10 seconds per request
    - Retry policy: 3 attempts with exponential backoff


## Configuration Management

- **Environment Variables:**
    - `RPC_ENDPOINT_URL`: Ankr RPC API URL
    - `RPC_TIMEOUT_SECONDS`: RPC request timeout (default: 10)
    - `RATE_LIMIT_PER_MINUTE`: Rate limit per IP (default: 1500)
    - `API_KEY_VALIDATION_ENABLED`: Enable API key validation (default: false)
- **Feature Flags:**
    - `CACHING_ENABLED`: Enable/disable response caching
    - `METRICS_ENABLED`: Enable/disable detailed metrics collection
- **Secrets Management:**
    - API keys stored in environment variables
    - RPC endpoint credentials (if any) in secure configuration


## Testing Requirements

- **Unit Test Coverage:** Minimum 85% code coverage
- **Integration Tests:**
    - Mock RPC API responses for various scenarios
    - Test error handling for RPC failures
    - Validate address extraction logic
- **Load Testing:**
    - N/A
- **Security Testing:**
    - Input validation testing
    - Rate limiting verification
    - SQL injection prevention (if applicable)


## Acceptance Criteria Implementation

- **AC1:** Service makes RPC calls to specified Ankr endpoint using `eth_getBlockByNumber`
- **AC2:** Extracts all unique addresses from transaction `from` and `to` fields
- **AC3:** Returns JSON response with unique address list and metadata
- **AC4:** Handles errors with appropriate HTTP status codes and error messages


## Definition of Done

- REST API endpoint implemented with Spring Boot
- RPC integration with proper error handling and timeouts
- Unit and integration tests with 85%+ coverage
- API documentation updated (OpenAPI/Swagger)
- Docker containerization complete
- Deployed and tested on nginx-net network
- Load testing completed
- Security review passed
- Business Analyst acceptance testing completed


## Technical Architecture

### Service Components

1. **Controller Layer:** REST API endpoint handling
2. **Service Layer:** Business logic and address extraction
3. **RPC Client:** HTTP client for Ankr API communication
5. **Configuration:** Environment-based configuration management

### Data Flow

1. Client sends GET request with block height
2. Controller validates input parameters
4. If not cached, make RPC call to Ankr API
5. Parse RPC response and extract addresses

