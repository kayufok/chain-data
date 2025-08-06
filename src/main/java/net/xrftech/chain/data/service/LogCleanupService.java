package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogCleanupService {
    
    private final AtomicLong totalClearedBytes = new AtomicLong(0);
    private final AtomicLong cleanupExecutions = new AtomicLong(0);
    
    /**
     * Clean up logs every 10 minutes to prevent OOM
     * - Force garbage collection
     * - Clean old log files
     * - Report memory status
     */
    @Scheduled(fixedRate = 600000) // 10 minutes = 600,000 milliseconds
    public void performLogCleanup() {
        long executionCount = cleanupExecutions.incrementAndGet();
        log.info("Starting log cleanup #{} at {}", executionCount, 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            // Get memory status before cleanup
            Runtime runtime = Runtime.getRuntime();
            long beforeUsed = runtime.totalMemory() - runtime.freeMemory();
            long beforeTotal = runtime.totalMemory();
            
            // Clean up old log files
            cleanupLogFiles();
            
            // Force garbage collection to free memory
            System.gc();
            
            // Wait a moment for GC to complete
            Thread.sleep(1000);
            
            // Get memory status after cleanup
            long afterUsed = runtime.totalMemory() - runtime.freeMemory();
            long afterTotal = runtime.totalMemory();
            long freedMemory = beforeUsed - afterUsed;
            
            log.info("Log cleanup #{} completed. Memory: {} MB -> {} MB (freed: {} MB), Total cleared: {} MB", 
                    executionCount,
                    beforeUsed / (1024 * 1024), 
                    afterUsed / (1024 * 1024),
                    freedMemory / (1024 * 1024),
                    totalClearedBytes.get() / (1024 * 1024));
            
        } catch (Exception e) {
            log.error("Error during log cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clean up old log files to prevent disk space issues
     */
    private void cleanupLogFiles() {
        try {
            String logDir = "logs";
            Path logPath = Paths.get(logDir);
            
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
                return;
            }
            
            File[] logFiles = logPath.toFile().listFiles((dir, name) -> 
                    name.endsWith(".log") || name.contains(".log."));
            
            if (logFiles != null && logFiles.length > 0) {
                // Sort by last modified time (oldest first)
                Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));
                
                long totalSize = Arrays.stream(logFiles).mapToLong(File::length).sum();
                long maxTotalSize = 100 * 1024 * 1024; // 100 MB total limit
                
                // Remove oldest files if total size exceeds limit
                if (totalSize > maxTotalSize) {
                    long sizeToFree = totalSize - maxTotalSize;
                    long freedSize = 0;
                    
                    for (File file : logFiles) {
                        if (freedSize >= sizeToFree) break;
                        
                        if (!file.getName().equals("chain-data.log")) { // Don't delete current log
                            long fileSize = file.length();
                            if (file.delete()) {
                                freedSize += fileSize;
                                totalClearedBytes.addAndGet(fileSize);
                                log.info("Deleted old log file: {} ({} bytes)", file.getName(), fileSize);
                            }
                        }
                    }
                }
                
                // Also remove files older than 7 days
                long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                for (File file : logFiles) {
                    if (file.lastModified() < sevenDaysAgo && !file.getName().equals("chain-data.log")) {
                        long fileSize = file.length();
                        if (file.delete()) {
                            totalClearedBytes.addAndGet(fileSize);
                            log.info("Deleted old log file (7+ days): {} ({} bytes)", file.getName(), fileSize);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error cleaning up log files: {}", e.getMessage());
        }
    }
    
    /**
     * Clear console buffer and reset streams to prevent memory accumulation
     */
    private void clearConsoleBuffer() {
        try {
            // This helps reduce memory usage from console output accumulation
            System.out.flush();
            System.err.flush();
            
            // Request garbage collection to clean up memory
            Runtime.getRuntime().runFinalization();
            
        } catch (Exception e) {
            log.debug("Error clearing console buffer: {}", e.getMessage());
        }
    }
    
    /**
     * Get cleanup statistics
     */
    public String getCleanupStats() {
        return String.format("Log cleanups executed: %d, Total bytes cleared: %d MB", 
                cleanupExecutions.get(), totalClearedBytes.get() / (1024 * 1024));
    }
    
    /**
     * Manual cleanup trigger for testing or emergency situations
     */
    public void triggerManualCleanup() {
        log.info("Manual log cleanup triggered");
        performLogCleanup();
    }
}