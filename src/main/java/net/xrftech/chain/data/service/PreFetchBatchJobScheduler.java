package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreFetchBatchJobScheduler {
    
    private final PreFetchBatchProcessorService preFetchBatchProcessorService;
    private final BatchProcessingProperties properties;
    
    /**
     * Scheduled pre-fetch batch processing job
     * Runs every 10 seconds by default, configurable via properties
     */
    @Scheduled(cron = "${batch.processing.schedule-cron:*/10 * * * * *}")
    public void scheduledPreFetchBatchProcessing() {
        if (!properties.isPrefetchEnabled()) {
            log.debug("Pre-fetch batch processing is disabled, skipping scheduled execution");
            return;
        }
        
        log.info("Scheduled pre-fetch batch processing triggered");
        
        try {
            // Directly call processBatch - it has its own concurrency control
            // This eliminates the race condition between isRunning() check and thread creation
            preFetchBatchProcessorService.processBatch();
            
        } catch (Exception e) {
            log.error("Failed to start scheduled pre-fetch batch processing: {}", e.getMessage(), e);
        }
    }
}
