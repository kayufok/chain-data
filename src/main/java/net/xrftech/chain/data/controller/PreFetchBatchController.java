package net.xrftech.chain.data.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.service.BatchMetricsService;
import net.xrftech.chain.data.service.PreFetchBatchProcessorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prefetch-batch")
@RequiredArgsConstructor
@Slf4j
public class PreFetchBatchController {
    
    private final PreFetchBatchProcessorService preFetchBatchProcessorService;
    
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
     * Start pre-fetch batch processing manually
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startPreFetchBatchProcessing() {
        log.info("Manual pre-fetch batch processing start requested");
        
        try {
            if (preFetchBatchProcessorService.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Pre-fetch batch processing is already running"));
            }
            
            // Start processing in a separate thread to avoid blocking
            new Thread(() -> {
                try {
                    preFetchBatchProcessorService.processBatch();
                } catch (Exception e) {
                    log.error("Error in pre-fetch batch processing: {}", e.getMessage(), e);
                }
            }).start();
            
            return ResponseEntity.ok(ApiResponse.success("Pre-fetch batch processing started"));
            
        } catch (Exception e) {
            log.error("Error starting pre-fetch batch processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start pre-fetch batch processing: " + e.getMessage()));
        }
    }
    
    /**
     * Stop current pre-fetch batch processing
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<String>> stopPreFetchBatchProcessing() {
        log.info("Manual pre-fetch batch processing stop requested");
        
        try {
            if (!preFetchBatchProcessorService.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("No pre-fetch batch processing is currently running"));
            }
            
            preFetchBatchProcessorService.requestStop();
            return ResponseEntity.ok(ApiResponse.success("Stop request sent to pre-fetch batch processor"));
            
        } catch (Exception e) {
            log.error("Error stopping pre-fetch batch processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to stop pre-fetch batch processing: " + e.getMessage()));
        }
    }
    
    /**
     * Get current pre-fetch batch job status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<BatchMetricsService.BatchMetrics>> getPreFetchBatchStatus() {
        try {
            BatchMetricsService.BatchMetrics metrics = preFetchBatchProcessorService.getMetrics();
            return ResponseEntity.ok(ApiResponse.success(metrics));
            
        } catch (Exception e) {
            log.error("Error getting pre-fetch batch status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get pre-fetch batch status: " + e.getMessage()));
        }
    }
    
    /**
     * Get pre-fetch processing metrics (alias for status)
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<BatchMetricsService.BatchMetrics>> getPreFetchProcessingMetrics() {
        return getPreFetchBatchStatus();
    }
    
    /**
     * Health check endpoint for pre-fetch batch processing
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            boolean isRunning = preFetchBatchProcessorService.isRunning();
            String status = isRunning ? "Pre-fetch batch processing is running" : "Pre-fetch batch processing is idle";
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("Error in pre-fetch batch health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Health check failed: " + e.getMessage()));
        }
    }
}
