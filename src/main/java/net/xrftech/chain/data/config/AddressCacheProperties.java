package net.xrftech.chain.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blockchain.address.cache")
public class AddressCacheProperties {

    /** Enable/disable the in-memory address cache */
    private boolean enabled = true;

    /** Maximum number of entries allowed in the cache */
    private int maxSize = 1_000_000;

    /** Default counter value assigned to a new address */
    private int defaultValue = 50;

    /** Decrement amount applied during decay */
    private int decayAmount = 2;

    /** Whether to use LRU eviction when decay does not free enough space */
    private boolean lruEvictionEnabled = true;

    /** Optional logging level for cache-specific logs (INFO by default) */
    private String loggingLevel = "INFO";

    /** Number of entries to evict in one batch for better performance */
    private int batchEvictionSize = 10_000;

    /** Enable memory usage monitoring and adaptive sizing */
    private boolean memoryCheckEnabled = true;

    /** Target memory usage percentage (80% of available heap) */
    private int targetMemoryPercent = 80;

    /** Minimum cache size to maintain even under memory pressure */
    private int minCacheSize = 100_000;
}



