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
    
    private final AtomicLong blocksProcessed = new AtomicLong(0);
    private final AtomicLong addressesFound = new AtomicLong(0);
    private final AtomicInteger failedBlocks = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong totalBatches = new AtomicLong(0);
    
    private volatile BatchJobStatus currentJobStatus = BatchJobStatus.IDLE;
    private volatile LocalDateTime jobStartTime;
    private volatile LocalDateTime jobEndTime;
    private volatile long currentBlockNumber = 0;
    private volatile int blocksInCurrentBatch = 0;
    private volatile int batchSize = 100;
    
    @Data
    public static class BatchMetrics {
        private BatchJobStatus jobStatus;
        private long currentBlockNumber;
        private long blocksProcessed;
        private int blocksRemaining;
        private long addressesFound;
        private int failedBlocks;
        private LocalDateTime estimatedCompletionTime;
        private String processingRate;
        private LocalDateTime jobStartTime;
        private LocalDateTime jobEndTime;
        private long totalBatches;
        private int consecutiveFailures;
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
        this.batchSize = batchSize;
        this.blocksInCurrentBatch = 0;
        
        log.info("Batch job started: block {} with batch size {}", startingBlockNumber, batchSize);
        this.currentJobStatus = BatchJobStatus.RUNNING;
    }
    
    /**
     * Stop the current batch job
     */
    public void stopJob() {
        this.currentJobStatus = BatchJobStatus.STOPPING;
        this.jobEndTime = LocalDateTime.now();
        log.info("Batch job stopped at block {}", currentBlockNumber);
        this.currentJobStatus = BatchJobStatus.STOPPED;
    }
    
    /**
     * Complete the current batch job
     */
    public void completeJob() {
        this.currentJobStatus = BatchJobStatus.COMPLETED;
        this.jobEndTime = LocalDateTime.now();
        this.totalBatches.incrementAndGet();
        log.info("Batch job completed. Processed {} blocks, found {} addresses", 
                blocksProcessed.get(), addressesFound.get());
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
        this.currentBlockNumber = blockNumber;
        this.blocksProcessed.incrementAndGet();
        this.addressesFound.addAndGet(addressCount);
        this.blocksInCurrentBatch++;
        this.consecutiveFailures.set(0); // Reset consecutive failures on success
        
        if (blocksProcessed.get() % 10 == 0) {
            log.info("Progress: Processed {} blocks, found {} addresses, current block: {}", 
                    blocksProcessed.get(), addressesFound.get(), blockNumber);
        }
    }
    
    /**
     * Record a failed block
     */
    public void recordBlockFailed(long blockNumber, String errorMessage) {
        this.currentBlockNumber = blockNumber;
        this.failedBlocks.incrementAndGet();
        this.consecutiveFailures.incrementAndGet();
        
        log.warn("Block {} failed: {}. Consecutive failures: {}", 
                blockNumber, errorMessage, consecutiveFailures.get());
    }
    
    /**
     * Get current metrics
     */
    public BatchMetrics getCurrentMetrics() {
        BatchMetrics metrics = new BatchMetrics();
        metrics.setJobStatus(currentJobStatus);
        metrics.setCurrentBlockNumber(currentBlockNumber);
        metrics.setBlocksProcessed(blocksProcessed.get());
        metrics.setBlocksRemaining(Math.max(0, batchSize - blocksInCurrentBatch));
        metrics.setAddressesFound(addressesFound.get());
        metrics.setFailedBlocks(failedBlocks.get());
        metrics.setJobStartTime(jobStartTime);
        metrics.setJobEndTime(jobEndTime);
        metrics.setTotalBatches(totalBatches.get());
        metrics.setConsecutiveFailures(consecutiveFailures.get());
        
        // Calculate processing rate
        if (jobStartTime != null && blocksProcessed.get() > 0) {
            long minutesElapsed = java.time.Duration.between(jobStartTime, LocalDateTime.now()).toMinutes();
            if (minutesElapsed > 0) {
                double rate = (double) blocksProcessed.get() / minutesElapsed;
                metrics.setProcessingRate(String.format("%.1f blocks/minute", rate));
                
                // Estimate completion time
                int remainingBlocks = metrics.getBlocksRemaining();
                if (remainingBlocks > 0 && rate > 0) {
                    long minutesToComplete = (long) (remainingBlocks / rate);
                    metrics.setEstimatedCompletionTime(LocalDateTime.now().plusMinutes(minutesToComplete));
                }
            } else {
                metrics.setProcessingRate("Calculating...");
            }
        } else {
            metrics.setProcessingRate("0 blocks/minute");
        }
        
        return metrics;
    }
    
    /**
     * Reset all metrics for a new job
     */
    public void resetMetrics() {
        blocksProcessed.set(0);
        addressesFound.set(0);
        failedBlocks.set(0);
        consecutiveFailures.set(0);
        currentBlockNumber = 0;
        blocksInCurrentBatch = 0;
        jobStartTime = null;
        jobEndTime = null;
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