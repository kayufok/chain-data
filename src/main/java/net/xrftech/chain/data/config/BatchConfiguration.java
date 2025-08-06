package net.xrftech.chain.data.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BatchConfiguration {
    
    private final BatchProcessingProperties properties;
    
    // Rate limiter is now configured as a Component with @PostConstruct
}