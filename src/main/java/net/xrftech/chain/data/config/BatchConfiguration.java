package net.xrftech.chain.data.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Slf4j
public class BatchConfiguration {
    
    // Rate limiter is now configured as a Component with @PostConstruct
}