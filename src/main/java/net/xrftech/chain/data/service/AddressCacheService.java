package net.xrftech.chain.data.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.AddressCacheProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressCacheService {

    private final AddressCacheProperties properties;

    // Core storage: address -> score
    private final ConcurrentHashMap<String, AtomicInteger> addressToScore = new ConcurrentHashMap<>();

    // Simple LRU tracking using a deque of keys; guarded by intrinsic lock
    private final Deque<String> lruOrder = new ArrayDeque<>();

    // Metrics
    @Getter private final AtomicInteger cacheHits = new AtomicInteger(0);
    @Getter private final AtomicInteger cacheMisses = new AtomicInteger(0);
    @Getter private final AtomicInteger skippedDbOps = new AtomicInteger(0);

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Check if address exists; if hit, increment score and record metrics. */
    public boolean checkAndBoostIfPresent(String address) {
        if (!properties.isEnabled()) {
            return false;
        }

        AtomicInteger score = addressToScore.get(address);
        if (score != null) {
            int newVal = score.addAndGet(properties.getDefaultValue());
            cacheHits.incrementAndGet();
            skippedDbOps.incrementAndGet();
            touchLru(address);
            if (log.isDebugEnabled()) {
                log.debug("Address cache HIT {} -> {}", address, newVal);
            }
            return true;
        } else {
            cacheMisses.incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("Address cache MISS {}", address);
            }
            return false;
        }
    }

    /** Add a new address with default score; performs decay/eviction if needed. */
    public void addIfAbsent(String address) {
        if (!properties.isEnabled()) {
            return;
        }

        if (addressToScore.size() >= properties.getMaxSize()) {
            performDecayAndEviction();
            if (addressToScore.size() >= properties.getMaxSize()) {
                // Still full; honor requirement to prioritize existing entries
                return;
            }
        }

        addressToScore.computeIfAbsent(address, k -> {
            touchLru(k);
            return new AtomicInteger(properties.getDefaultValue());
        });
    }

    /** Bulk add convenience. */
    public void addAllIfAbsent(Set<String> addresses) {
        if (!properties.isEnabled() || addresses == null || addresses.isEmpty()) {
            return;
        }
        for (String address : addresses) {
            addIfAbsent(address);
        }
    }

    /** Decay all entries and remove those at or below zero. */
    public void performDecayAndEviction() {
        if (!properties.isEnabled()) {
            return;
        }

        int before = addressToScore.size();

        // Decay pass
        for (Map.Entry<String, AtomicInteger> entry : addressToScore.entrySet()) {
            AtomicInteger val = entry.getValue();
            int after = val.addAndGet(-properties.getDecayAmount());
            if (after <= 0) {
                addressToScore.remove(entry.getKey(), val);
                removeFromLru(entry.getKey());
            }
        }

        // Enhanced batch LRU eviction if still full
        if (properties.isLruEvictionEnabled() && addressToScore.size() >= properties.getMaxSize()) {
            int toEvict = Math.max(0, addressToScore.size() - properties.getMaxSize() + properties.getBatchEvictionSize());
            evictBatch(toEvict);
        }
        
        // Memory-based eviction if enabled
        if (properties.isMemoryCheckEnabled()) {
            performMemoryBasedEviction();
        }

        int after = addressToScore.size();
        if (log.isInfoEnabled()) {
            log.info("Address cache decay: size {} -> {}", before, after);
        }
    }

    private void touchLru(String key) {
        if (!properties.isLruEvictionEnabled()) return;
        synchronized (lruOrder) {
            lruOrder.remove(key);
            lruOrder.addLast(key);
        }
    }

    private void removeFromLru(String key) {
        if (!properties.isLruEvictionEnabled()) return;
        synchronized (lruOrder) {
            lruOrder.remove(key);
        }
    }

    private String pollOldest() {
        if (!properties.isLruEvictionEnabled()) return null;
        synchronized (lruOrder) {
            return lruOrder.pollFirst();
        }
    }
    
    /**
     * Efficiently evict multiple entries in batch
     */
    private void evictBatch(int toEvict) {
        int evicted = 0;
        synchronized (lruOrder) {
            while (evicted < toEvict && !lruOrder.isEmpty()) {
                String oldest = lruOrder.pollFirst();
                if (oldest != null && addressToScore.remove(oldest) != null) {
                    evicted++;
                }
            }
        }
        
        if (evicted > 0) {
            log.debug("Batch evicted {} entries from cache", evicted);
        }
    }
    
    /**
     * Memory-based eviction when memory usage is high
     */
    private void performMemoryBasedEviction() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        // Calculate memory usage percentage
        double memoryUsage = (double) usedMemory / maxMemory * 100;
        double targetUsage = properties.getTargetMemoryPercent();
        
        if (memoryUsage > targetUsage && addressToScore.size() > properties.getMinCacheSize()) {
            // Aggressive eviction when memory is high
            int currentSize = addressToScore.size();
            int targetSize = Math.max(properties.getMinCacheSize(), (int) (currentSize * 0.8)); // Reduce by 20%
            int toEvict = currentSize - targetSize;
            
            if (toEvict > 0) {
                evictBatch(toEvict);
                log.info("Memory-based eviction: removed {} entries, memory usage: {:.1f}%", 
                        toEvict, memoryUsage);
            }
        }
    }
    
    /**
     * Get memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return new MemoryStats(
                usedMemory / (1024 * 1024),  // MB
                maxMemory / (1024 * 1024),   // MB
                freeMemory / (1024 * 1024),  // MB
                (double) usedMemory / maxMemory * 100
        );
    }

    public CacheStats getStatsSnapshot() {
        return new CacheStats(
                addressToScore.size(),
                properties.getMaxSize(),
                cacheHits.get(),
                cacheMisses.get(),
                skippedDbOps.get(),
                utilizationPercent()
        );
    }

    public void resetBatchCounters() {
        // Reset only per-batch metrics; keep global size and entries
        cacheHits.set(0);
        cacheMisses.set(0);
        skippedDbOps.set(0);
    }

    private int utilizationPercent() {
        if (properties.getMaxSize() <= 0) return 0;
        return (int) Math.round((addressToScore.size() * 100.0) / properties.getMaxSize());
    }

    @Getter
    public static class CacheStats {
        private final int size;
        private final int maxSize;
        private final int hits;
        private final int misses;
        private final int skippedDbOperations;
        private final int utilizationPercent;

        public CacheStats(int size, int maxSize, int hits, int misses, int skippedDbOperations, int utilizationPercent) {
            this.size = size;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.skippedDbOperations = skippedDbOperations;
            this.utilizationPercent = utilizationPercent;
        }
    }
    
    @Getter
    public static class MemoryStats {
        private final long usedMemoryMB;
        private final long maxMemoryMB;
        private final long freeMemoryMB;
        private final double memoryUsagePercent;

        public MemoryStats(long usedMemoryMB, long maxMemoryMB, long freeMemoryMB, double memoryUsagePercent) {
            this.usedMemoryMB = usedMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.freeMemoryMB = freeMemoryMB;
            this.memoryUsagePercent = memoryUsagePercent;
        }
    }
}


