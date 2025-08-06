-- Migration V001: Initial Database Schema
-- Description: Create all core tables for blockchain data storage

-- Address Table
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    wallet_address TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chain Info Table
CREATE TABLE chain_info (
    id BIGSERIAL PRIMARY KEY,
    chain_name TEXT NOT NULL,
    chain_id TEXT NOT NULL UNIQUE,
    next_block_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Address Chain Junction Table
CREATE TABLE address_chain (
    id BIGSERIAL PRIMARY KEY,
    wallet_address_id BIGINT NOT NULL REFERENCES address(id) ON DELETE CASCADE,
    chain_id BIGINT NOT NULL REFERENCES chain_info(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(wallet_address_id, chain_id)
);

-- Status Reference Table
CREATE TABLE status (
    id BIGSERIAL PRIMARY KEY,
    status_type TEXT NOT NULL,
    status_code TEXT NOT NULL UNIQUE,
    status_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- API Call Failure Log Table
CREATE TABLE api_call_failure_log (
    id BIGSERIAL PRIMARY KEY,
    chain_id TEXT NOT NULL REFERENCES chain_info(chain_id),
    block_number BIGINT NOT NULL,
    status_code TEXT NOT NULL REFERENCES status(status_code),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_address_wallet_address ON address(wallet_address);
CREATE INDEX idx_chain_info_chain_id ON chain_info(chain_id);
CREATE INDEX idx_address_chain_wallet_id ON address_chain(wallet_address_id);
CREATE INDEX idx_address_chain_chain_id ON address_chain(chain_id);
CREATE INDEX idx_status_status_code ON status(status_code);
CREATE INDEX idx_failure_log_chain_id ON api_call_failure_log(chain_id);
CREATE INDEX idx_failure_log_block_number ON api_call_failure_log(block_number);

-- Insert initial data
INSERT INTO status (status_type, status_code, status_description) VALUES
('API_ERROR', 'RPC_TIMEOUT', 'RPC request timed out'),
('API_ERROR', 'RPC_CONNECTION_FAILED', 'Failed to connect to RPC endpoint'),
('API_ERROR', 'INVALID_BLOCK', 'Invalid block number or block not found'),
('API_ERROR', 'RATE_LIMIT_EXCEEDED', 'API rate limit exceeded'),
('SUCCESS', 'BLOCK_PROCESSED', 'Block processed successfully');

-- Insert initial chain data
INSERT INTO chain_info (chain_name, chain_id, next_block_number) VALUES
('Ethereum', '1', 18500000),
('Polygon', '137', 50000000),
('BSC', '56', 35000000);