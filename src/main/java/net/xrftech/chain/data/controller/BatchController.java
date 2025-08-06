package net.xrftech.chain.data.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.service.BatchMetricsService;
import net.xrftech.chain.data.service.EthereumBatchProcessorService;
import net.xrftech.chain.data.service.LogCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
    
    private final EthereumBatchProcessorService batchProcessorService;
    private final LogCleanupService logCleanupService;
    
    @Data
    public static class ApiResponse<T> {
        private String status;
        private T data;
        private String message;
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setStatus("success");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setStatus("error");
            response.setMessage(message);
            return response;
        }
    }
    
    /**
     * Start batch processing manually
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startBatchProcessing() {
        log.info("Manual batch processing start requested");
        
        try {
            if (batchProcessorService.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Batch processing is already running"));
            }
            
            // Run batch processing asynchronously to avoid blocking the API call
            CompletableFuture.runAsync(() -> {
                try {
                    batchProcessorService.processBatch();
                } catch (Exception e) {
                    log.error("Error in manual batch processing: {}", e.getMessage(), e);
                }
            });
            
            return ResponseEntity.ok(ApiResponse.success("Batch processing started"));
            
        } catch (Exception e) {
            log.error("Error starting batch processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start batch processing: " + e.getMessage()));
        }
    }
    
    /**
     * Stop current batch processing
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<String>> stopBatchProcessing() {
        log.info("Manual batch processing stop requested");
        
        try {
            if (!batchProcessorService.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No batch processing is currently running"));
            }
            
            batchProcessorService.requestStop();
            return ResponseEntity.ok(ApiResponse.success("Stop request sent to batch processor"));
            
        } catch (Exception e) {
            log.error("Error stopping batch processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to stop batch processing: " + e.getMessage()));
        }
    }
    
    /**
     * Get current batch job status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<BatchMetricsService.BatchMetrics>> getBatchStatus() {
        try {
            BatchMetricsService.BatchMetrics metrics = batchProcessorService.getMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
            
        } catch (Exception e) {
            log.error("Error getting batch status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get batch status: " + e.getMessage()));
        }
    }
    
    /**
     * Get processing metrics (alias for status)
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<BatchMetricsService.BatchMetrics>> getProcessingMetrics() {
        return getBatchStatus();
    }
    
    /**
     * Health check endpoint for batch processing
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            boolean isRunning = batchProcessorService.isRunning();
            String status = isRunning ? "Batch processing is running" : "Batch processing is idle";
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("Error in batch health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Health check failed: " + e.getMessage()));
        }
    }
    
    /**
     * Trigger manual log cleanup
     */
    @PostMapping("/cleanup-logs")
    public ResponseEntity<ApiResponse<String>> cleanupLogs() {
        try {
            logCleanupService.triggerManualCleanup();
            return ResponseEntity.ok(ApiResponse.success("Log cleanup completed"));
            
        } catch (Exception e) {
            log.error("Error during manual log cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Log cleanup failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get log cleanup statistics
     */
    @GetMapping("/cleanup-stats")
    public ResponseEntity<ApiResponse<String>> getCleanupStats() {
        try {
            String stats = logCleanupService.getCleanupStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("Error getting cleanup stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get cleanup stats: " + e.getMessage()));
        }
    }
    
    /**
     * Get current memory status
     */
    @GetMapping("/memory-status")
    public ResponseEntity<ApiResponse<String>> getMemoryStatus() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            String memoryInfo = String.format(
                    "Used: %d MB, Free: %d MB, Total: %d MB, Max: %d MB (%.1f%% used)",
                    usedMemory / (1024 * 1024),
                    freeMemory / (1024 * 1024),
                    totalMemory / (1024 * 1024),
                    maxMemory / (1024 * 1024),
                    (double) usedMemory / totalMemory * 100
            );
            
            return ResponseEntity.ok(ApiResponse.success(memoryInfo));
            
        } catch (Exception e) {
            log.error("Error getting memory status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get memory status: " + e.getMessage()));
        }
    }
}