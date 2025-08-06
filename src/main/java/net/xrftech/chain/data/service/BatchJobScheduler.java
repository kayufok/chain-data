package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "batch.processing.enabled", havingValue = "true", matchIfMissing = true)
public class BatchJobScheduler {
    
    private final EthereumBatchProcessorService batchProcessorService;
    private final BatchProcessingProperties properties;
    
    /**
     * Scheduled batch processing job
     * Default: every 5 minutes (0 star/5 * * * *)
     */
    @Scheduled(cron = "${batch.processing.schedule-cron:0 */5 * * * *}")
    public void scheduledBatchProcessing() {
        if (!properties.isEnabled()) {
            log.debug("Batch processing is disabled, skipping scheduled execution");
            return;
        }
        
        try {
            log.info("Starting scheduled batch processing");
            batchProcessorService.processBatch();
            log.info("Scheduled batch processing completed");
            
        } catch (Exception e) {
            log.error("Error in scheduled batch processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Health check for the scheduler
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void healthCheck() {
        if (properties.isEnabled()) {
            log.debug("Batch job scheduler is running, next execution: {}", 
                    properties.getScheduleCron());
        }
    }
}