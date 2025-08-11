package net.xrftech.chain.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "batch.processing")
public class BatchProcessingProperties {
    
    /**
     * Number of blocks to process per batch
     */
    private int batchSize = 150;
    
    /**
     * API requests per minute rate limit
     */
    private int rateLimitPerMinute = 1500;
    
    /**
     * Maximum retry attempts for failed API calls
     */
    private int retryAttempts = 3;
    
    /**
     * Cron expression for batch job schedule (default: every 5 minutes)
     */
    private String scheduleCron = "0 */5 * * * *";
    
    /**
     * Target blockchain chain ID (default: Ethereum mainnet)
     */
    private String chainId = "1";
    
    /**
     * Enable or disable batch processing
     */
    private boolean enabled = true;
    
    /**
     * Enable detailed metrics collection
     */
    private boolean metricsEnabled = true;
    
    /**
     * Enable alerting for failures
     */
    private boolean alertsEnabled = false;
    
    /**
     * Use bulk insert operations for better performance
     */
    private boolean bulkInsertEnabled = true;
    
    /**
     * Maximum number of consecutive failures before stopping processing
     */
    private int maxConsecutiveFailures = 10;
    
    /**
     * Delay between retries in milliseconds
     */
    private long retryDelayMs = 1000;
    
    /**
     * Maximum number of concurrent RPC calls for pre-fetch processing
     */
    private int maxConcurrentRpcCalls = 10;
    
    /**
     * Enable pre-fetch batch processing
     */
    private boolean prefetchEnabled = true;
    
    /**
     * Memory threshold in MB for batch processing
     */
    private int memoryThresholdMb = 1800;
}