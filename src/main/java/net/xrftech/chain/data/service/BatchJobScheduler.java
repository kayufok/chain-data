package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "batch.processing.enabled", havingValue = "true", matchIfMissing = true)
public class BatchJobScheduler {
    
    private final EthereumBatchProcessorService batchProcessorService;
    private final BatchProcessingProperties properties;
    
    // Flag to track if a batch job is currently running
    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);
    
    /**
     * Scheduled batch processing job
     * Runs every 10 seconds, skips if previous job is still running
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void scheduledBatchProcessing() {
        if (!properties.isEnabled()) {
            log.debug("Batch processing is disabled, skipping scheduled execution");
            return;
        }
        
        // Check if a job is already running
        if (!isJobRunning.compareAndSet(false, true)) {
            log.debug("Previous batch job is still running, skipping this execution");
            return;
        }
        
        try {
            log.info("Starting scheduled batch processing");
            batchProcessorService.processBatch();
            log.info("Scheduled batch processing completed");
            
        } catch (Exception e) {
            log.error("Error in scheduled batch processing: {}", e.getMessage(), e);
        } finally {
            // Always reset the flag when job completes (success or failure)
            isJobRunning.set(false);
        }
    }
    
    /**
     * Health check for the scheduler
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void healthCheck() {
        if (properties.isEnabled()) {
            log.debug("Batch job scheduler is running, job currently active: {}", 
                    isJobRunning.get());
        }
    }
    
    /**
     * Check if a batch job is currently running
     */
    public boolean isJobCurrentlyRunning() {
        return isJobRunning.get();
    }
}