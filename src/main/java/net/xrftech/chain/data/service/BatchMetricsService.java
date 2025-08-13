package net.xrftech.chain.data.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class BatchMetricsService {
    
    // Cumulative metrics across all batches
    private final AtomicLong totalBlocksProcessed = new AtomicLong(0);
    private final AtomicLong totalAddressesFound = new AtomicLong(0);
    private final AtomicInteger totalFailedBlocks = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong totalBatchesCompleted = new AtomicLong(0);
    
    // Job-level tracking
    private volatile BatchJobStatus currentJobStatus = BatchJobStatus.IDLE;
    private volatile LocalDateTime jobStartTime;
    private volatile LocalDateTime jobEndTime;
    
    // Current batch tracking
    private volatile long currentBatchNumber = 0;
    private volatile long currentBlockNumber = 0;
    private volatile int currentBatchSize = 200;    // default batch size
    private volatile String currentBatchPhase = "Idle";
    private volatile LocalDateTime currentBatchStartTime;
    private volatile LocalDateTime currentBatchEndTime;
    
    // Phase timing tracking
    private volatile LocalDateTime preFetchStartTime;
    private volatile LocalDateTime preFetchEndTime;
    private volatile LocalDateTime dbActivityStartTime;
    private volatile LocalDateTime dbActivityEndTime;
    private volatile LocalDateTime cacheUpdateStartTime;
    private volatile LocalDateTime cacheUpdateEndTime;
    
    // Last completed phase durations
    private volatile String lastPreFetchDuration;
    private volatile String lastDbActivityDuration;
    private volatile String lastCacheUpdateDuration;
    
    // Performance tracking
    private volatile int activeBatchCount = 0;
    private final AtomicLong totalBatchDurationMs = new AtomicLong(0);
    
    @Data
    public static class BatchMetrics {
        // Job-level status (only when different from batch)
        private BatchJobStatus jobStatus;
        private LocalDateTime jobStartTime;
        private LocalDateTime jobEndTime;
        private String totalJobDuration;  // renamed from jobDuration
        
        // Current batch info
        private long currentBatchNumber;
        private long currentBlockNumber;
        private int currentBatchSize;      // total blocks in current batch
        private String currentBatchPhase;  // "Pre-fetch", "Storage", "Cache Update", "Completed"
        private LocalDateTime currentBatchStartTime;
        private String currentBatchDuration;
        
        // Phase timing breakdown
        private String lastPreFetchDuration;
        private String lastDbActivityDuration;
        private String lastCacheUpdateDuration;
        
        // Cumulative metrics (since job start)
        private long totalBlocksProcessed;
        private long totalAddressesFound;
        private long totalBatchesCompleted;
        private int totalFailedBlocks;
        private int consecutiveFailures;
        
        // Performance metrics
        private String averageBatchDuration;
        private String blocksPerSecond;
        private String addressesPerSecond;
        private String estimatedTimeRemaining;
        
        // System status
        private boolean multipleBatchesRunning;
        private int activeBatchCount;
        
        // Cache metrics (US-006)
        private Integer cacheSize;
        private Integer cacheMaxSize;
        private Integer cacheHitRatePercent;
        private Integer cacheHits;
        private Integer cacheMisses;
        private Integer cacheSkippedDbOps;
        private Integer cacheUtilizationPercent;
    }
    
    public enum BatchJobStatus {
        IDLE, STARTING, RUNNING, STOPPING, STOPPED, ERROR, COMPLETED
    }
    
    /**
     * Start a new batch job
     */
    public void startJob(long startingBlockNumber, int batchSize) {
        this.currentJobStatus = BatchJobStatus.STARTING;
        this.jobStartTime = LocalDateTime.now();
        this.jobEndTime = null;
        this.currentBlockNumber = startingBlockNumber;
        this.currentBatchSize = batchSize;
        
        log.info("Batch job started: block {} with batch size {}", startingBlockNumber, batchSize);
        this.currentJobStatus = BatchJobStatus.RUNNING;
    }
    

    
    /**
     * Start a new individual batch
     */
    public void startBatch(long startingBlockNumber, int batchSize) {
        this.currentBatchNumber++;
        this.currentBlockNumber = startingBlockNumber;
        this.currentBatchSize = batchSize;
        this.currentBatchPhase = "Starting";
        this.currentBatchStartTime = LocalDateTime.now();
        this.currentBatchEndTime = null;
        this.activeBatchCount++;
        
        // Reset phase timings
        this.preFetchStartTime = null;
        this.preFetchEndTime = null;
        this.dbActivityStartTime = null;
        this.dbActivityEndTime = null;
        this.cacheUpdateStartTime = null;
        this.cacheUpdateEndTime = null;
        
        log.info("Batch #{} started: processing {} blocks starting from {}", 
                currentBatchNumber, batchSize, startingBlockNumber);
    }
    
    /**
     * Start pre-fetch phase
     */
    public void startPreFetchPhase() {
        this.currentBatchPhase = "Pre-fetch";
        this.preFetchStartTime = LocalDateTime.now();
        log.info("Batch #{}: Pre-fetch phase started", currentBatchNumber);
    }
    
    /**
     * Complete pre-fetch phase
     */
    public void completePreFetchPhase() {
        this.preFetchEndTime = LocalDateTime.now();
        if (preFetchStartTime != null) {
            long durationMs = java.time.Duration.between(preFetchStartTime, preFetchEndTime).toMillis();
            this.lastPreFetchDuration = formatDuration(durationMs / 1000);
        }
        log.info("Batch #{}: Pre-fetch phase completed in {}", currentBatchNumber, lastPreFetchDuration);
    }
    
    /**
     * Start database activity phase
     */
    public void startDbActivityPhase() {
        this.currentBatchPhase = "Storage";
        this.dbActivityStartTime = LocalDateTime.now();
        log.info("Batch #{}: Database activity phase started", currentBatchNumber);
    }
    
    /**
     * Complete database activity phase
     */
    public void completeDbActivityPhase() {
        this.dbActivityEndTime = LocalDateTime.now();
        if (dbActivityStartTime != null) {
            long durationMs = java.time.Duration.between(dbActivityStartTime, dbActivityEndTime).toMillis();
            this.lastDbActivityDuration = formatDuration(durationMs / 1000);
        }
        log.info("Batch #{}: Database activity phase completed in {}", currentBatchNumber, lastDbActivityDuration);
    }
    
    /**
     * Start cache update phase
     */
    public void startCacheUpdatePhase() {
        this.currentBatchPhase = "Cache Update";
        this.cacheUpdateStartTime = LocalDateTime.now();
        log.info("Batch #{}: Cache update phase started", currentBatchNumber);
    }
    
    /**
     * Complete cache update phase
     */
    public void completeCacheUpdatePhase() {
        this.cacheUpdateEndTime = LocalDateTime.now();
        if (cacheUpdateStartTime != null) {
            long durationMs = java.time.Duration.between(cacheUpdateStartTime, cacheUpdateEndTime).toMillis();
            this.lastCacheUpdateDuration = formatDuration(durationMs / 1000);
        }
        log.info("Batch #{}: Cache update phase completed in {}", currentBatchNumber, lastCacheUpdateDuration);
    }
    
    /**
     * Complete the current individual batch
     */
    public void completeBatch() {
        this.currentBatchPhase = "Completed";
        this.currentBatchEndTime = LocalDateTime.now();
        if (currentBatchStartTime != null) {
            long durationMs = java.time.Duration.between(currentBatchStartTime, currentBatchEndTime).toMillis();
            totalBatchDurationMs.addAndGet(durationMs);
        }
        this.totalBatchesCompleted.incrementAndGet();
        this.activeBatchCount = Math.max(0, activeBatchCount - 1);
        
        log.info("Batch #{} completed: processed {} blocks in {} ms (Pre-fetch: {}, DB: {}, Cache: {})", 
                currentBatchNumber, currentBatchSize, 
                currentBatchEndTime != null && currentBatchStartTime != null ? 
                java.time.Duration.between(currentBatchStartTime, currentBatchEndTime).toMillis() : 0,
                lastPreFetchDuration, lastDbActivityDuration, lastCacheUpdateDuration);
    }
    
    /**
     * Stop the current batch job
     */
    public void stopJob() {
        this.currentJobStatus = BatchJobStatus.STOPPING;
        this.jobEndTime = LocalDateTime.now();
        log.info("Batch job stopped, next block to process: {}", currentBlockNumber);
        this.currentJobStatus = BatchJobStatus.STOPPED;
    }
    
    /**
     * Complete the current batch job
     */
    public void completeJob() {
        this.currentJobStatus = BatchJobStatus.COMPLETED;
        this.jobEndTime = LocalDateTime.now();
        // currentBlockNumber already represents the next block to be processed
        log.info("Batch job completed. Processed {} blocks, found {} addresses, completed {} batches", 
                totalBlocksProcessed.get(), totalAddressesFound.get(), totalBatchesCompleted.get());
    }
    
    /**
     * Mark job as error state
     */
    public void errorJob(String errorMessage) {
        this.currentJobStatus = BatchJobStatus.ERROR;
        this.jobEndTime = LocalDateTime.now();
        log.error("Batch job encountered error: {}", errorMessage);
    }
    
    /**
     * Record a successfully processed block
     */
    public void recordBlockProcessed(long blockNumber, int addressCount) {
        this.currentBlockNumber = blockNumber + 1; // Set to next block to be processed
        this.totalBlocksProcessed.incrementAndGet();
        this.totalAddressesFound.addAndGet(addressCount);

        this.consecutiveFailures.set(0); // Reset consecutive failures on success
        
        if (totalBlocksProcessed.get() % 50 == 0) {
            log.info("Progress: Batch #{} - {} blocks processed in current job, {} addresses found", 
                    currentBatchNumber, totalBlocksProcessed.get(), totalAddressesFound.get());
        }
    }
    
    /**
     * Record a failed block
     */
    public void recordBlockFailed(long blockNumber, String errorMessage) {
        this.currentBlockNumber = blockNumber + 1; // Set to next block to be processed
        this.totalFailedBlocks.incrementAndGet();
        this.consecutiveFailures.incrementAndGet();
        
        log.warn("Block {} failed: {}. Consecutive failures: {}", 
                blockNumber, errorMessage, consecutiveFailures.get());
    }
    
    /**
     * Get current metrics with improved clarity
     */
    public BatchMetrics getCurrentMetrics() {
        BatchMetrics metrics = new BatchMetrics();
        LocalDateTime now = LocalDateTime.now();
        
        // Job-level status
        metrics.setJobStatus(currentJobStatus);
        metrics.setJobStartTime(jobStartTime);
        metrics.setJobEndTime(jobEndTime);
        
        if (jobStartTime != null) {
            LocalDateTime endTime = jobEndTime != null ? jobEndTime : now;
            long totalJobSeconds = java.time.Duration.between(jobStartTime, endTime).getSeconds();
            metrics.setTotalJobDuration(formatDuration(totalJobSeconds));
        }
        
        // Current batch info
        metrics.setCurrentBatchNumber(currentBatchNumber);
        metrics.setCurrentBlockNumber(currentBlockNumber);

        metrics.setCurrentBatchSize(currentBatchSize);

        metrics.setCurrentBatchStartTime(currentBatchStartTime);
        
        if (currentBatchStartTime != null) {
            LocalDateTime batchEndTime = currentBatchEndTime != null ? currentBatchEndTime : now;
            long currentBatchSeconds = java.time.Duration.between(currentBatchStartTime, batchEndTime).getSeconds();
            metrics.setCurrentBatchDuration(formatDuration(currentBatchSeconds));
        }
        
        // Cumulative metrics
        metrics.setTotalBlocksProcessed(totalBlocksProcessed.get());
        metrics.setTotalAddressesFound(totalAddressesFound.get());
        metrics.setTotalBatchesCompleted(totalBatchesCompleted.get());
        metrics.setTotalFailedBlocks(totalFailedBlocks.get());
        metrics.setConsecutiveFailures(consecutiveFailures.get());
        
        // Performance metrics
        long completedBatches = totalBatchesCompleted.get();
        if (completedBatches > 0 && totalBatchDurationMs.get() > 0) {
            long avgBatchMs = totalBatchDurationMs.get() / completedBatches;
            metrics.setAverageBatchDuration(formatDuration(avgBatchMs / 1000));
        }
        
        // Calculate rates
        if (jobStartTime != null && totalBlocksProcessed.get() > 0) {
            long totalSeconds = java.time.Duration.between(jobStartTime, now).getSeconds();
            if (totalSeconds > 0) {
                double blocksPerSec = (double) totalBlocksProcessed.get() / totalSeconds;
                double addressesPerSec = (double) totalAddressesFound.get() / totalSeconds;
                metrics.setBlocksPerSecond(String.format("%.2f blocks/sec", blocksPerSec));
                metrics.setAddressesPerSecond(String.format("%.1f addresses/sec", addressesPerSec));
                
                // For pre-fetch batches, estimate remaining time based on current phase
                if ("Pre-fetch".equals(currentBatchPhase) || "Storage".equals(currentBatchPhase) || "Cache Update".equals(currentBatchPhase)) {
                    // Estimate based on average batch duration
                    long completedBatchesForEstimate = totalBatchesCompleted.get();
                    if (completedBatchesForEstimate > 0 && totalBatchDurationMs.get() > 0) {
                        long avgBatchMs = totalBatchDurationMs.get() / completedBatchesForEstimate;
                        long currentBatchMs = java.time.Duration.between(currentBatchStartTime != null ? currentBatchStartTime : now, now).toMillis();
                        long estimatedRemainingMs = Math.max(0, avgBatchMs - currentBatchMs);
                        metrics.setEstimatedTimeRemaining(formatDuration(estimatedRemainingMs / 1000));
                    }
                }
            }
        }
        
        // System status
        metrics.setActiveBatchCount(activeBatchCount);
        metrics.setMultipleBatchesRunning(activeBatchCount > 1);
        
        return metrics;
    }
    
    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm %ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
    }
    
    /**
     * Reset all metrics for a new job
     */
    public void resetMetrics() {
        totalBlocksProcessed.set(0);
        totalAddressesFound.set(0);
        totalFailedBlocks.set(0);
        consecutiveFailures.set(0);
        totalBatchesCompleted.set(0);
        totalBatchDurationMs.set(0);
        
        currentBatchNumber = 0;
        currentBlockNumber = 0;
        currentBatchPhase = "Idle";
        activeBatchCount = 0;
        
        jobStartTime = null;
        jobEndTime = null;
        currentBatchStartTime = null;
        currentBatchEndTime = null;
        
        // Reset phase timings
        preFetchStartTime = null;
        preFetchEndTime = null;
        dbActivityStartTime = null;
        dbActivityEndTime = null;
        cacheUpdateStartTime = null;
        cacheUpdateEndTime = null;
        lastPreFetchDuration = null;
        lastDbActivityDuration = null;
        lastCacheUpdateDuration = null;
        
        currentJobStatus = BatchJobStatus.IDLE;
        
        log.info("Batch metrics reset");
    }
    
    /**
     * Check if job should be stopped due to consecutive failures
     */
    public boolean shouldStopDueToFailures(int maxConsecutiveFailures) {
        return consecutiveFailures.get() >= maxConsecutiveFailures;
    }
    
    /**
     * Get current job status
     */
    public BatchJobStatus getCurrentJobStatus() {
        return currentJobStatus;
    }
    
    /**
     * Check if job is currently running
     */
    public boolean isJobRunning() {
        return currentJobStatus == BatchJobStatus.RUNNING || 
               currentJobStatus == BatchJobStatus.STARTING;
    }
}