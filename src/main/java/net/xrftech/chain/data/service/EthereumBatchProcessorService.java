package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import net.xrftech.chain.data.dto.BlockAddressResponse;
import net.xrftech.chain.data.entity.Address;
import net.xrftech.chain.data.entity.AddressChain;
import net.xrftech.chain.data.entity.ApiCallFailureLog;
import net.xrftech.chain.data.entity.ChainInfo;
import net.xrftech.chain.data.mapper.AddressChainMapper;
import net.xrftech.chain.data.mapper.AddressMapper;
import net.xrftech.chain.data.mapper.ApiCallFailureLogMapper;
import net.xrftech.chain.data.mapper.ChainInfoMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EthereumBatchProcessorService {
    
    private final EthereumBlockService ethereumBlockService;
    private final RateLimiter rateLimiter;
    private final BatchMetricsService metricsService;
    private final AddressCacheService addressCacheService;
    private final BatchProcessingProperties properties;
    private final BulkInsertService bulkInsertService;
    
    private final AddressMapper addressMapper;
    private final AddressChainMapper addressChainMapper;
    private final ApiCallFailureLogMapper failureLogMapper;
    private final ChainInfoMapper chainInfoMapper;
    
    private volatile boolean stopRequested = false;
    
    /**
     * Process a batch of blocks starting from the next block number
     */
    public void processBatch() {
        if (metricsService.isJobRunning()) {
            log.warn("Batch job already running, skipping this execution");
            return;
        }
        
        try {
            // Get chain info to determine starting block
            ChainInfo chainInfo = getChainInfo();
            if (chainInfo == null) {
                log.error("Chain info not found for chain ID: {}", properties.getChainId());
                return;
            }
            
            long startBlock = chainInfo.getNextBlockNumber();
            int batchSize = properties.getBatchSize();
            
            log.info("Starting batch processing from block {} with batch size {}", startBlock, batchSize);
            metricsService.startJob(startBlock, batchSize);
            
            // Reset stop flag
            stopRequested = false;
            
            // Process blocks in batch
            for (int i = 0; i < batchSize && !stopRequested; i++) {
                long currentBlock = startBlock + i;
                
                try {
                    // Apply rate limiting
                    rateLimiter.acquire();
                    
                    // Process single block
                    processBlock(currentBlock, chainInfo.getId());
                    
                } catch (InterruptedException e) {
                    log.warn("Batch processing interrupted at block {}", currentBlock);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing block {}: {}", currentBlock, e.getMessage(), e);
                    recordFailure(currentBlock, e.getMessage());
                    
                    // Check if we should stop due to consecutive failures
                    if (metricsService.shouldStopDueToFailures(properties.getMaxConsecutiveFailures())) {
                        log.error("Stopping batch processing due to {} consecutive failures", 
                                properties.getMaxConsecutiveFailures());
                        metricsService.errorJob("Too many consecutive failures");
                        return;
                    }
                }
            }
            
            // Update next block number regardless of success/failure
            updateNextBlockNumber(chainInfo, startBlock + batchSize);
            
            if (stopRequested) {
                metricsService.stopJob();
            } else {
                metricsService.completeJob();
            }
            
        } catch (Exception e) {
            log.error("Critical error in batch processing: {}", e.getMessage(), e);
            metricsService.errorJob(e.getMessage());
        }
    }
    
    /**
     * Process a single block and extract addresses
     */
    private void processBlock(long blockNumber, Long chainInfoId) {
        try {
            log.debug("Processing block: {}", blockNumber);
            
            // Get block data using existing service
            BlockAddressResponse blockData = ethereumBlockService.getBlockAddresses(String.valueOf(blockNumber));
            
            if (blockData != null && blockData.getData() != null && 
                blockData.getData().getAddresses() != null && !blockData.getData().getAddresses().isEmpty()) {
                // Extract unique addresses
                Set<String> uniqueAddresses = new HashSet<>(blockData.getData().getAddresses());
                
                // Store addresses and relationships using optimized bulk operations
                bulkInsertService.bulkInsertAddressesAndRelationships(uniqueAddresses, chainInfoId);
                
                // Record success metrics
                metricsService.recordBlockProcessed(blockNumber, uniqueAddresses.size());
                
                log.debug("Successfully processed block {} with {} unique addresses", 
                        blockNumber, uniqueAddresses.size());
            } else {
                // Block processed but no addresses found
                metricsService.recordBlockProcessed(blockNumber, 0);
                log.debug("Block {} processed with no addresses", blockNumber);
            }
            
        } catch (Exception e) {
            log.error("Failed to process block {}: {}", blockNumber, e.getMessage());
            recordFailure(blockNumber, e.getMessage());
            // Don't re-throw to avoid transaction rollback
        }
    }
    
    /**
     * Store addresses and their chain relationships
     */
    @Transactional
    public void storeAddressesAndRelationships(Set<String> addresses, Long chainInfoId) {
        if (addresses.isEmpty()) {
            return;
        }
        
        try {
            // Bulk insert addresses (ignore duplicates)
            List<Address> addressEntities = addresses.stream()
                    .map(addr -> Address.builder()
                            .walletAddress(addr)
                            .build())
                    .collect(Collectors.toList());
            
            // Insert addresses in batches to avoid memory issues
            int batchSize = 100;
            for (int i = 0; i < addressEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, addressEntities.size());
                List<Address> batch = addressEntities.subList(i, endIndex);
                
                for (Address address : batch) {
                    try {
                        addressMapper.insert(address);
                    } catch (Exception e) {
                        // Address already exists, get the existing one
                        Address existing = addressMapper.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Address>()
                                        .eq("wallet_address", address.getWalletAddress())
                        );
                        if (existing != null) {
                            address.setId(existing.getId());
                        } else {
                            // If we can't find the existing address, log the error but don't fail the transaction
                            log.warn("Failed to insert address {} and couldn't find existing: {}", 
                                    address.getWalletAddress(), e.getMessage());
                        }
                    }
                }
            }
            
            // Create address-chain relationships
            List<AddressChain> relationships = new ArrayList<>();
            for (Address address : addressEntities) {
                if (address.getId() != null) {
                    AddressChain relationship = AddressChain.builder()
                            .walletAddressId(address.getId())
                            .chainId(chainInfoId)
                            .build();
                    relationships.add(relationship);
                }
            }
            
            // Insert relationships (ignore duplicates)
            for (AddressChain relationship : relationships) {
                try {
                    addressChainMapper.insert(relationship);
                } catch (Exception e) {
                    // Relationship already exists, ignore
                    log.debug("Address-chain relationship already exists: addressId={}, chainId={}", 
                            relationship.getWalletAddressId(), relationship.getChainId());
                }
            }
            
            log.debug("Stored {} addresses and {} relationships", addresses.size(), relationships.size());
            
        } catch (Exception e) {
            log.error("Error storing addresses and relationships: {}", e.getMessage(), e);
            // Don't re-throw the exception to avoid transaction rollback
        }
    }
    
    /**
     * Record API call failure
     */
    private void recordFailure(long blockNumber, String errorMessage) {
        try {
            ApiCallFailureLog failureLog = ApiCallFailureLog.builder()
                    .chainId(properties.getChainId())
                    .blockNumber(blockNumber)
                    .statusCode("BATCH_PROCESSING_ERROR")
                    .errorMessage(errorMessage)
                    .build();
            
            failureLogMapper.insert(failureLog);
            metricsService.recordBlockFailed(blockNumber, errorMessage);
            
        } catch (Exception e) {
            log.error("Failed to record failure log: {}", e.getMessage());
            // Don't re-throw to avoid transaction rollback
        }
    }
    
    /**
     * Update next block number in chain info
     */
    @Transactional
    public void updateNextBlockNumber(ChainInfo chainInfo, long nextBlockNumber) {
        try {
            chainInfo.setNextBlockNumber(nextBlockNumber);
            chainInfo.setUpdatedAt(LocalDateTime.now());
            chainInfoMapper.updateById(chainInfo);
            
            log.info("Updated next block number to {} for chain {}", nextBlockNumber, chainInfo.getChainName());
            
        } catch (Exception e) {
            log.error("Failed to update next block number: {}", e.getMessage());
            // Don't re-throw to avoid transaction rollback
        }
    }
    
    /**
     * Get chain info for the configured chain ID
     */
    private ChainInfo getChainInfo() {
        try {
            return chainInfoMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChainInfo>()
                            .eq("chain_id", properties.getChainId())
            );
        } catch (Exception e) {
            log.error("Failed to get chain info for chain ID {}: {}", properties.getChainId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Request stop of current batch processing
     */
    public void requestStop() {
        log.info("Stop requested for batch processing");
        stopRequested = true;
    }
    
    /**
     * Check if batch processing is currently running
     */
    public boolean isRunning() {
        return metricsService.isJobRunning();
    }
    
    /**
     * Get current batch processing metrics
     */
    public BatchMetricsService.BatchMetrics getMetrics() {
        BatchMetricsService.BatchMetrics metrics = metricsService.getCurrentMetrics();
        AddressCacheService.CacheStats stats = addressCacheService.getStatsSnapshot();
        int totalLookups = stats.getHits() + stats.getMisses();
        int hitRate = totalLookups == 0 ? 0 : (int) Math.round((stats.getHits() * 100.0) / totalLookups);

        metrics.setCacheSize(stats.getSize());
        metrics.setCacheMaxSize(stats.getMaxSize());
        metrics.setCacheHits(stats.getHits());
        metrics.setCacheMisses(stats.getMisses());
        metrics.setCacheSkippedDbOps(stats.getSkippedDbOperations());
        metrics.setCacheUtilizationPercent(stats.getUtilizationPercent());
        metrics.setCacheHitRatePercent(hitRate);
        return metrics;
    }
}