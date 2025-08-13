package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkInsertService {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Bulk insert addresses using PostgreSQL's ON CONFLICT for high performance
     * Returns map of wallet_address -> address_id for relationship creation
     */
    @Transactional
    public Map<String, Long> bulkInsertAddresses(Set<String> addresses) {
        if (addresses.isEmpty()) {
            return Collections.emptyMap();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Create the bulk INSERT with ON CONFLICT statement
            String sql = """
                INSERT INTO address (wallet_address, created_at, updated_at) 
                VALUES (?, ?, ?) 
                ON CONFLICT (wallet_address) DO NOTHING
                """;
            
            // Prepare batch parameters
            List<Object[]> batchArgs = addresses.stream()
                    .map(address -> new Object[]{
                            address,
                            Timestamp.valueOf(LocalDateTime.now()),
                            Timestamp.valueOf(LocalDateTime.now())
                    })
                    .collect(Collectors.toList());
            
            // Execute batch insert with error resilience
            Map<String, Long> addressIdMap = executeResilientBatchInsert(sql, batchArgs, addresses);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Bulk inserted {} addresses in {} ms ({} addresses/sec)", 
                    addresses.size(), duration, 
                    duration > 0 ? (addresses.size() * 1000L / duration) : "∞");
            
            return addressIdMap;
            
        } catch (Exception e) {
            log.error("Error in bulk insert addresses: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Execute batch insert with resilience - if batch fails, retry individual inserts
     */
    private Map<String, Long> executeResilientBatchInsert(String sql, List<Object[]> batchArgs, Set<String> addresses) {
        try {
            // Try batch insert first (fastest approach)
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Batch insert successful for {} addresses", addresses.size());
            
            // Get IDs for all addresses (both newly inserted and existing)
            return getAddressIds(addresses);
            
        } catch (Exception batchException) {
            log.error("Address batch insert failed. This indicates a serious database issue: {}", batchException.getMessage());
            throw new RuntimeException("Bulk address insert failed", batchException);
        }
    }
    
    /**
     * Fallback method: Execute individual inserts when batch fails
     * Uses separate transactions to avoid "transaction aborted" errors
     */
    private Map<String, Long> executeIndividualInserts(String sql, List<Object[]> batchArgs, Set<String> addresses) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failedAddresses = new ArrayList<>();
        List<String> addressList = new ArrayList<>(addresses);
        
        for (int i = 0; i < batchArgs.size(); i++) {
            Object[] args = batchArgs.get(i);
            String address = addressList.get(i);
            
            try {
                // Execute each insert in its own transaction context
                jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                    try (var ps = connection.prepareStatement(sql)) {
                        ps.setString(1, (String) args[0]);
                        ps.setTimestamp(2, (java.sql.Timestamp) args[1]);
                        ps.setTimestamp(3, (java.sql.Timestamp) args[2]);
                        ps.executeUpdate();
                        return null;
                    }
                });
                successCount++;
                log.debug("Individual insert successful for address: {}", address);
                
            } catch (Exception e) {
                failureCount++;
                failedAddresses.add(address);
                // Only log first few failures to avoid spam
                if (failureCount <= 5) {
                    log.warn("Individual insert failed for address {}: {}", address, e.getMessage());
                } else if (failureCount == 6) {
                    log.warn("Suppressing further individual insert failure logs...");
                }
            }
        }
        
        log.info("Individual insert results: {} successful, {} failed out of {} total addresses", 
                successCount, failureCount, addresses.size());
        
        if (!failedAddresses.isEmpty()) {
            log.error("Failed to insert {} addresses: {}", failedAddresses.size(), 
                    failedAddresses.size() > 10 ? 
                    failedAddresses.subList(0, 10) + "... (showing first 10)" : 
                    failedAddresses);
        }
        
        // Get IDs for successfully inserted addresses only
        Set<String> successfulAddresses = new HashSet<>(addresses);
        successfulAddresses.removeAll(failedAddresses);
        
        return getAddressIds(successfulAddresses);
    }
    
    /**
     * Get address IDs for given wallet addresses
     */
    private Map<String, Long> getAddressIds(Set<String> addresses) {
        if (addresses.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Create IN clause with placeholders
        String inClause = addresses.stream()
                .map(addr -> "?")
                .collect(Collectors.joining(","));
        
        String sql = "SELECT id, wallet_address FROM address WHERE wallet_address IN (" + inClause + ")";
        
        // Execute query
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, addresses.toArray());
        
        // Convert to map
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("wallet_address"),
                        row -> ((Number) row.get("id")).longValue()
                ));
    }
    
    /**
     * Bulk insert address-chain relationships
     */
    @Transactional
    public void bulkInsertAddressChainRelationships(Map<String, Long> addressIdMap, Long chainInfoId) {
        if (addressIdMap.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            String sql = """
                INSERT INTO address_chain (wallet_address_id, chain_id, created_at) 
                VALUES (?, ?, ?) 
                ON CONFLICT (wallet_address_id, chain_id) DO NOTHING
                """;
            
            // Prepare batch parameters
            List<Object[]> batchArgs = addressIdMap.values().stream()
                    .map(addressId -> new Object[]{
                            addressId,
                            chainInfoId,
                            Timestamp.valueOf(LocalDateTime.now())
                    })
                    .collect(Collectors.toList());
            
            // Execute batch insert with resilience
            executeResilientBatchInsertRelationships(sql, batchArgs, addressIdMap);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Bulk inserted {} address-chain relationships in {} ms ({} relationships/sec)", 
                    addressIdMap.size(), duration,
                    duration > 0 ? (addressIdMap.size() * 1000L / duration) : "∞");
            
        } catch (Exception e) {
            log.error("Error in bulk insert address-chain relationships: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Execute batch insert for relationships with resilience
     * Uses separate transactions to handle failures properly
     */
    private void executeResilientBatchInsertRelationships(String sql, List<Object[]> batchArgs, Map<String, Long> addressIdMap) {
        try {
            // Try batch insert first (fastest approach)
            jdbcTemplate.batchUpdate(sql, batchArgs);
            log.debug("Batch insert successful for {} relationships", batchArgs.size());
            
        } catch (Exception batchException) {
            log.error("Relationship batch insert failed. This indicates a serious database issue: {}", batchException.getMessage());
            throw new RuntimeException("Bulk relationship insert failed", batchException);
        }
    }
    

    
    /**
     * Combined bulk operation for addresses and relationships
     */
    @Transactional
    public void bulkInsertAddressesAndRelationships(Set<String> addresses, Long chainInfoId) {
        if (addresses.isEmpty()) {
            return;
        }
        
        long totalStartTime = System.currentTimeMillis();
        
        // Step 1: Bulk insert addresses
        Map<String, Long> addressIdMap = bulkInsertAddresses(addresses);
        
        // Step 2: Bulk insert relationships
        bulkInsertAddressChainRelationships(addressIdMap, chainInfoId);
        
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        log.info("Complete bulk operation for {} addresses completed in {} ms", 
                addresses.size(), totalDuration);
    }
    
    /**
     * Optimize database settings for bulk operations
     */
    public void optimizeForBulkOperations() {
        try {
            // Temporarily optimize settings for bulk operations
            jdbcTemplate.execute("SET work_mem = '256MB'");
            jdbcTemplate.execute("SET maintenance_work_mem = '512MB'");
            jdbcTemplate.execute("SET synchronous_commit = 'off'");
            jdbcTemplate.execute("SET wal_buffers = '64MB'");
            
            log.info("Database optimized for bulk operations");
        } catch (Exception e) {
            log.warn("Failed to optimize database settings: {}", e.getMessage());
        }
    }
    
    /**
     * Reset database settings after bulk operations
     */
    public void resetOptimizations() {
        try {
            jdbcTemplate.execute("RESET work_mem");
            jdbcTemplate.execute("RESET maintenance_work_mem");
            jdbcTemplate.execute("RESET synchronous_commit");
            jdbcTemplate.execute("RESET wal_buffers");
            
            log.info("Database settings reset to defaults");
        } catch (Exception e) {
            log.warn("Failed to reset database settings: {}", e.getMessage());
        }
    }
}
