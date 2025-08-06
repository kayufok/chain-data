package net.xrftech.chain.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {
    
    private final BatchProcessingProperties properties;
    
    private final AtomicLong tokens = new AtomicLong();
    private final AtomicLong lastRefillTime = new AtomicLong();
    private long maxTokens;
    private long refillRate; // tokens per second
    
    @PostConstruct
    public void init() {
        int requestsPerMinute = properties.getRateLimitPerMinute();
        this.maxTokens = Math.max(1, requestsPerMinute / 60); // Convert to per second
        this.refillRate = this.maxTokens;
        this.tokens.set(maxTokens);
        this.lastRefillTime.set(Instant.now().getEpochSecond());
        log.info("Rate limiter initialized: {} requests per minute ({} per second)", 
                requestsPerMinute, this.maxTokens);
    }

    
    /**
     * Attempts to acquire a token for making an API request
     * @return true if token acquired, false if rate limit exceeded
     */
    public boolean tryAcquire() {
        refillTokens();
        
        long currentTokens = tokens.get();
        if (currentTokens > 0) {
            if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                log.debug("Token acquired, remaining: {}", currentTokens - 1);
                return true;
            }
        }
        
        log.warn("Rate limit exceeded, no tokens available");
        return false;
    }
    
    /**
     * Blocks until a token is available
     * @throws InterruptedException if interrupted while waiting
     */
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            // Wait for token refill (check every 100ms)
            Thread.sleep(100);
        }
    }
    
    /**
     * Gets the current number of available tokens
     */
    public long getAvailableTokens() {
        refillTokens();
        return tokens.get();
    }
    
    /**
     * Calculates delay needed before next request can be made
     * @return delay in milliseconds, 0 if token is available
     */
    public long getDelayUntilNextRequest() {
        if (tryAcquire()) {
            // Put the token back since this is just checking
            tokens.incrementAndGet();
            return 0;
        }
        
        // Calculate when next token will be available
        long tokensNeeded = 1;
        long delaySeconds = tokensNeeded * (1000 / refillRate); // Convert to milliseconds
        return Math.max(100, delaySeconds); // Minimum 100ms delay
    }
    
    /**
     * Refills tokens based on elapsed time
     */
    private void refillTokens() {
        long now = Instant.now().getEpochSecond();
        long lastRefill = lastRefillTime.get();
        
        if (now > lastRefill) {
            long elapsedSeconds = now - lastRefill;
            long tokensToAdd = elapsedSeconds * refillRate;
            
            if (tokensToAdd > 0) {
                long currentTokens = tokens.get();
                long newTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
                
                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    lastRefillTime.set(now);
                    log.debug("Refilled {} tokens, current: {}", tokensToAdd, newTokens);
                }
            }
        }
    }
    
    /**
     * Resets the rate limiter with new configuration
     */
    public void reconfigure(int requestsPerMinute) {
        long newMaxTokens = Math.max(1, requestsPerMinute / 60);
        log.info("Reconfiguring rate limiter: {} requests per minute ({} per second)", 
                requestsPerMinute, newMaxTokens);
        
        tokens.set(newMaxTokens);
        lastRefillTime.set(Instant.now().getEpochSecond());
    }
}