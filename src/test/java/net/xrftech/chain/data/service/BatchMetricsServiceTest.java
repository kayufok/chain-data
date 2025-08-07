package net.xrftech.chain.data.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BatchMetricsServiceTest {

    private BatchMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new BatchMetricsService();
    }

    @Test
    void testCurrentBlockNumberIncrementsCorrectly() {
        // Start a job from block 46299
        metricsService.startJob(46299, 100);
        
        // Verify initial state
        BatchMetricsService.BatchMetrics metrics = metricsService.getCurrentMetrics();
        assertEquals(46299, metrics.getCurrentBlockNumber());
        
        // Process a few blocks
        metricsService.recordBlockProcessed(46299, 5);
        metrics = metricsService.getCurrentMetrics();
        assertEquals(46300, metrics.getCurrentBlockNumber()); // Should be next block
        
        metricsService.recordBlockProcessed(46300, 3);
        metrics = metricsService.getCurrentMetrics();
        assertEquals(46301, metrics.getCurrentBlockNumber()); // Should be next block
        
        // Complete the job
        metricsService.completeJob();
        metrics = metricsService.getCurrentMetrics();
        assertEquals(46301, metrics.getCurrentBlockNumber()); // Should remain as next block to process
        assertEquals(BatchMetricsService.BatchJobStatus.COMPLETED, metrics.getJobStatus());
    }

    @Test
    void testFailedBlockAlsoIncrementsCurrentBlockNumber() {
        metricsService.startJob(46299, 100);
        
        // Process a block successfully
        metricsService.recordBlockProcessed(46299, 5);
        BatchMetricsService.BatchMetrics metrics = metricsService.getCurrentMetrics();
        assertEquals(46300, metrics.getCurrentBlockNumber());
        
        // Fail a block
        metricsService.recordBlockFailed(46300, "API error");
        metrics = metricsService.getCurrentMetrics();
        assertEquals(46301, metrics.getCurrentBlockNumber()); // Should still increment to next block
    }
}
