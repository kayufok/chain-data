package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import net.xrftech.chain.data.dto.BlockAddressResponse;
import net.xrftech.chain.data.entity.ApiCallFailureLog;
import net.xrftech.chain.data.entity.ChainInfo;
import net.xrftech.chain.data.mapper.AddressChainMapper;
import net.xrftech.chain.data.mapper.AddressMapper;
import net.xrftech.chain.data.mapper.ApiCallFailureLogMapper;
import net.xrftech.chain.data.mapper.ChainInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreFetchBatchProcessorService {
    
    private final EthereumBlockService ethereumBlockService;
    private final RateLimiter rateLimiter;
    private final BatchMetricsService metricsService;
    private final AddressCacheService addressCacheService;
    private final BatchProcessingProperties properties;
    private final BulkInsertService bulkInsertService;
    
    private final ApiCallFailureLogMapper failureLogMapper;
    private final ChainInfoMapper chainInfoMapper;
    

    
    private volatile boolean stopRequested = false;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    /**
     * Process a batch of blocks using pre-fetch strategy with concurrent RPC calls
     */
    public void processBatch() {
        // Atomic concurrency control - only one batch can run at a time
        if (!isProcessing.compareAndSet(false, true)) {
            log.warn("Batch processing already running, skipping this execution");
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
            
            log.info("Starting pre-fetch batch processing from block {} with batch size {}", startBlock, batchSize);
            
            // Start new job for this batch
            metricsService.startJob(startBlock, batchSize);
            
            // Each processBatch() call is one batch
            metricsService.startBatch(startBlock, batchSize);
            // Reset per-batch cache counters
            addressCacheService.resetBatchCounters();
            
            // Reset stop flag
            stopRequested = false;
            
            // Phase 1: Pre-fetch all blocks concurrently
            metricsService.startPreFetchPhase();
            Map<Long, Set<String>> blockAddresses = preFetchPhase(startBlock, batchSize);
            metricsService.completePreFetchPhase();
            
            if (stopRequested) {
                metricsService.stopJob();
                return;
            }
            
            // Phase 2: Storage phase with cache optimization
            metricsService.startDbActivityPhase();
            storagePhase(blockAddresses, chainInfo.getId());
            metricsService.completeDbActivityPhase();

            // Log cache metrics for this batch
            AddressCacheService.CacheStats cacheStats = addressCacheService.getStatsSnapshot();
            int lookups = cacheStats.getHits() + cacheStats.getMisses();
            int hitRate = lookups == 0 ? 0 : (int) Math.round((cacheStats.getHits() * 100.0) / lookups);
            log.info("Cache hit rate: {}%, Skipped operations: {}, Size: {}/{} ({}%)",
                    hitRate,
                    cacheStats.getSkippedDbOperations(),
                    cacheStats.getSize(),
                    cacheStats.getMaxSize(),
                    cacheStats.getUtilizationPercent());
            
            // Update next block number regardless of success/failure
            updateNextBlockNumber(chainInfo, startBlock + batchSize);
            
            // Always complete the batch
            metricsService.completeBatch();
            
            if (stopRequested) {
                log.info("Stop requested, stopping job");
                metricsService.stopJob();
            } else {
                // Complete job after each batch to allow next batch to start
                // Each batch run is treated as a separate job
                log.info("Completing job to allow next batch to start");
                metricsService.completeJob();
            }
            
        } catch (Exception e) {
            log.error("Critical error in pre-fetch batch processing: {}", e.getMessage(), e);
            metricsService.errorJob(e.getMessage());
        } finally {
            // Always release the processing lock
            isProcessing.set(false);
            log.info("Batch processing lock released, next batch can start");
        }
    }
    
    /**
     * Phase 1: Pre-fetch all blocks concurrently
     */
    private Map<Long, Set<String>> preFetchPhase(long startBlock, int batchSize) {
        log.info("Starting pre-fetch phase for blocks {} to {}", startBlock, startBlock + batchSize - 1);
        
        // Create list of block numbers to process
        List<Long> blockNumbers = new ArrayList<>();
        for (int i = 0; i < batchSize && !stopRequested; i++) {
            blockNumbers.add(startBlock + i);
        }
        
        // Execute concurrent RPC calls with rate limiting
        Map<Long, Set<String>> blockAddresses = new ConcurrentHashMap<>();
        List<Long> failedBlocks = new ArrayList<>();
        
        // Create executor service for concurrent processing
        ExecutorService executor = Executors.newFixedThreadPool(properties.getMaxConcurrentRpcCalls());
        
        try {
            // Submit all tasks
            List<CompletableFuture<Void>> futures = blockNumbers.stream()
                    .map(blockNumber -> CompletableFuture.runAsync(() -> {
                        try {
                            Set<String> addresses = processBlockConcurrently(blockNumber);
                            if (addresses != null && !addresses.isEmpty()) {
                                blockAddresses.put(blockNumber, addresses);
                            }
                        } catch (Exception e) {
                            log.error("Failed to process block {}: {}", blockNumber, e.getMessage());
                            failedBlocks.add(blockNumber);
                            recordFailure(blockNumber, e.getMessage());
                        }
                    }, executor))
                    .collect(Collectors.toList());
            
            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
        } finally {
            executor.shutdown();
        }
        
        log.info("Pre-fetch phase completed. Successfully processed {} blocks, {} failed blocks, collected {} unique addresses",
                blockAddresses.size(), failedBlocks.size(), 
                blockAddresses.values().stream().mapToInt(Set::size).sum());
        
        return blockAddresses;
    }
    
    /**
     * Process a single block concurrently
     */
    private Set<String> processBlockConcurrently(Long blockNumber) {
        try {
            // Apply rate limiting
            try {
                rateLimiter.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiter interrupted", e);
            }
            
            // Get block data using existing service
            BlockAddressResponse blockData = ethereumBlockService.getBlockAddresses(String.valueOf(blockNumber));
            
            if (blockData != null && blockData.getData() != null && 
                blockData.getData().getAddresses() != null && !blockData.getData().getAddresses().isEmpty()) {
                
                Set<String> addresses = new HashSet<>(blockData.getData().getAddresses());
                log.debug("Successfully processed block {} with {} unique addresses", blockNumber, addresses.size());
                return addresses;
            } else {
                log.debug("Block {} processed with no addresses", blockNumber);
                return Collections.emptySet();
            }
            
        } catch (Exception e) {
            log.error("Failed to process block {}: {}", blockNumber, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Phase 2: Storage phase - bulk operations
     */
    @Transactional
    private void storagePhase(Map<Long, Set<String>> blockAddresses, Long chainInfoId) {
        log.info("Starting storage phase for {} blocks", blockAddresses.size());
        
        try {
            // Collect all unique addresses from all blocks
            Set<String> allAddresses = blockAddresses.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            
            log.info("Collected {} unique addresses across all blocks", allAddresses.size());
            
            // Apply cache optimization: filter out addresses that are present in cache
            Set<String> addressesToPersist = new HashSet<>();
            for (String address : allAddresses) {
                boolean hit = addressCacheService.checkAndBoostIfPresent(address);
                if (!hit) {
                    addressesToPersist.add(address);
                }
            }

            // Bulk insert only the addresses not found in cache using optimized bulk operations
            if (!addressesToPersist.isEmpty()) {
                log.info("Starting bulk insert for {} new addresses", addressesToPersist.size());
                
                // Optimize database for bulk operations
                bulkInsertService.optimizeForBulkOperations();
                
                try {
                    // Use the new high-performance bulk insert service
                    bulkInsertService.bulkInsertAddressesAndRelationships(addressesToPersist, chainInfoId);
                    
                } finally {
                    // Reset database optimizations
                    bulkInsertService.resetOptimizations();
                }
                
                // Phase 3: Cache update phase
                metricsService.startCacheUpdatePhase();
                try {
                    // Add newly persisted addresses to cache with default counter
                    addressCacheService.addAllIfAbsent(addressesToPersist);
                } finally {
                    metricsService.completeCacheUpdatePhase();
                }
                
                log.info("Bulk insert completed for {} addresses", addressesToPersist.size());
            }
            
            // Record metrics for ALL blocks in the batch, including those with no addresses
            // We need to record all blocks that were attempted, not just those with addresses
            long startBlock = blockAddresses.keySet().stream().min(Long::compareTo).orElse(0L);
            int batchSize = properties.getBatchSize();
            
            for (long blockNumber = startBlock; blockNumber < startBlock + batchSize; blockNumber++) {
                Set<String> addresses = blockAddresses.getOrDefault(blockNumber, Collections.emptySet());
                metricsService.recordBlockProcessed(blockNumber, addresses.size());
            }
            
            log.info("Storage phase completed successfully. Stored {} addresses and relationships", allAddresses.size());
            
        } catch (Exception e) {
            log.error("Error in storage phase: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // Old inefficient bulk insert methods removed - now using BulkInsertService
    
    /**
     * Record API call failure
     */
    private void recordFailure(long blockNumber, String errorMessage) {
        try {
            ApiCallFailureLog failureLog = ApiCallFailureLog.builder()
                    .chainId(properties.getChainId())
                    .blockNumber(blockNumber)
                    .statusCode("PREFETCH_BATCH_PROCESSING_ERROR")
                    .errorMessage(errorMessage)
                    .build();
            
            failureLogMapper.insert(failureLog);
            metricsService.recordBlockFailed(blockNumber, errorMessage);
            
        } catch (Exception e) {
            log.error("Failed to record failure log: {}", e.getMessage());
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
        log.info("Stop requested for pre-fetch batch processing");
        stopRequested = true;
    }
    
    /**
     * Check if batch processing is currently running
     */
    public boolean isRunning() {
        return isProcessing.get();
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
