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
            if (preFetchBatchProcessorService.isRunning()) {
                log.warn("Pre-fetch batch processing already running, skipping scheduled execution");
                return;
            }
            
            // Start processing in a separate thread to avoid blocking the scheduler
            new Thread(() -> {
                try {
                    preFetchBatchProcessorService.processBatch();
                    log.info("Scheduled pre-fetch batch processing completed");
                } catch (Exception e) {
                    log.error("Error in scheduled pre-fetch batch processing: {}", e.getMessage(), e);
                }
            }).start();
            
        } catch (Exception e) {
            log.error("Failed to start scheduled pre-fetch batch processing: {}", e.getMessage(), e);
        }
    }
}
