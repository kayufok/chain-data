package net.xrftech.chain.data.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import net.xrftech.chain.data.config.BatchProcessingProperties;
import net.xrftech.chain.data.mapper.AddressMapper;
import net.xrftech.chain.data.mapper.AddressChainMapper;
import net.xrftech.chain.data.mapper.ApiCallFailureLogMapper;
import net.xrftech.chain.data.mapper.ChainInfoMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreFetchBatchProcessorServiceTest {

    @Mock
    private EthereumBlockService ethereumBlockService;
    
    @Mock
    private RateLimiter rateLimiter;
    
    @Mock
    private BatchMetricsService metricsService;
    
    @Mock
    private BatchProcessingProperties properties;
    
    @Mock
    private AddressMapper addressMapper;
    
    @Mock
    private AddressChainMapper addressChainMapper;
    
    @Mock
    private ApiCallFailureLogMapper failureLogMapper;
    
    @Mock
    private ChainInfoMapper chainInfoMapper;

    private PreFetchBatchProcessorService preFetchBatchProcessorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Configure default properties
        when(properties.getChainId()).thenReturn("1");
        when(properties.getBatchSize()).thenReturn(10);
        when(properties.getMaxConcurrentRpcCalls()).thenReturn(5);
        
        preFetchBatchProcessorService = new PreFetchBatchProcessorService(
                ethereumBlockService,
                rateLimiter,
                metricsService,
                properties,
                addressMapper,
                addressChainMapper,
                failureLogMapper,
                chainInfoMapper
        );
    }

    @Test
    void testPreFetchBatchProcessorServiceCreation() {
        assertNotNull(preFetchBatchProcessorService);
    }

    @Test
    void testIsRunning() {
        when(metricsService.isJobRunning()).thenReturn(false);
        assertFalse(preFetchBatchProcessorService.isRunning());
        
        when(metricsService.isJobRunning()).thenReturn(true);
        assertTrue(preFetchBatchProcessorService.isRunning());
    }

    @Test
    void testRequestStop() {
        assertFalse(preFetchBatchProcessorService.isRunning());
        preFetchBatchProcessorService.requestStop();
        // The stop flag is internal, so we just verify no exception is thrown
        assertTrue(true);
    }
}
